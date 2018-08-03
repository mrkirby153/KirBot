package me.mrkirby153.KirBot.server

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.DB
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Channel
import me.mrkirby153.KirBot.database.models.CustomCommand
import me.mrkirby153.KirBot.database.models.DiscordUser
import me.mrkirby153.KirBot.database.models.RoleClearance
import me.mrkirby153.KirBot.database.models.group.Group
import me.mrkirby153.KirBot.database.models.guild.CommandAlias
import me.mrkirby153.KirBot.database.models.guild.GuildMember
import me.mrkirby153.KirBot.database.models.guild.GuildMemberRole
import me.mrkirby153.KirBot.database.models.guild.MusicSettings
import me.mrkirby153.KirBot.database.models.guild.ServerSettings
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.music.MusicManager
import me.mrkirby153.KirBot.realname.RealnameHandler
import me.mrkirby153.KirBot.realname.RealnameSetting
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.user.CLEARANCE_GLOBAL_ADMIN
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.toTypedArray
import me.mrkirby153.kcutils.child
import me.mrkirby153.kcutils.mkdirIfNotExist
import me.mrkirby153.kcutils.use
import me.mrkirby153.kcutils.utils.IdGenerator
import me.xdrop.fuzzywuzzy.FuzzySearch
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.Semaphore

class KirBotGuild(val guild: Guild) : Guild by guild {

    init {
        Bot.LOG.debug("Constructing KirBotGuild for $guild")
    }

    lateinit var settings: ServerSettings
    lateinit var customCommands: MutableList<CustomCommand>
    lateinit var commandAliases: MutableList<CommandAlias>
    val clearances: MutableMap<String, Int> = mutableMapOf()


    val musicManager = MusicManager(this)
    val logManager = LogManager(this)

    var extraData = JSONObject()
    private val dataFile = Bot.files.data.child("servers").mkdirIfNotExist().child(
            "${this.id}.json")

    private val lock = Semaphore(1, true)
    private var lockedAt: Long = -1
    private var lockedThread: Thread? = null
    private var lockDump: Array<StackTraceElement> = arrayOf()
    private var lockDumped = false

    var ready = false

    fun lock() {
        Bot.LOG.debug("[LOCK] Lock on ${this.id} aquired by ${Thread.currentThread().name}")
        lock.acquire()
        lockedAt = System.currentTimeMillis()
        lockedThread = Thread.currentThread()
        lockDumped = false
        val stack = Thread.currentThread().stackTrace.toMutableList()
        stack.drop(2)
        lockDump = stack.toTypedArray()
    }

    fun unlock() {
        Bot.LOG.debug(
                "[LOCK] Lock on ${this.id} released after ${System.currentTimeMillis() - lockedAt} ms")
        lockedAt = -1
        lockedThread = null
        lock.release()
    }

    fun checkLeak() {
        if (lockedAt + leakThreshold < System.currentTimeMillis() && !lockDumped && lockedThread != null) {
            Bot.LOG.debug(
                    "[LEAK] Access to guild [${this.id}] may have leaked! (Locked $leakThreshold ms ago by \"${lockedThread?.name}\")")
            Bot.LOG.debug("Trace:")
            lockDump.forEach {
                Bot.LOG.debug("\t$it")
            }
            lockDumped = true
        }
    }

    fun loadSettings() {
        Bot.LOG.debug("Loading settings for ${this}")

        settings = Model.where(ServerSettings::class.java, "id",
                this.id).first() ?: throw IllegalStateException(
                "Attempting to load settings for a guild that doesn't exist")

        customCommands = Model.where(CustomCommand::class.java,
                "server", this.id).get().toMutableList()
        commandAliases = Model.where(CommandAlias::class.java, "server_id",
                this.id).get().toMutableList()
        logManager.reloadLogChannels()
        loadData()
    }

    fun onPart() {
        lock()
        dataFile.delete()
        ModuleManager[Redis::class.java].getConnection().use {
            it.del("data:${this.id}")
        }
        unlock()
        KirBotGuild.remove(this)
    }

    fun sync(waitFor: Boolean = false) {
        val keyGen = IdGenerator(IdGenerator.ALPHA + IdGenerator.NUMBERS)
        Bot.LOG.debug("Syncing guild ${this.id}")
        lock()
        var guild = Model.where(ServerSettings::class.java, "id", this.id).first()

        if (guild == null) {
            Bot.LOG.debug("Guild does not exist... Creating")
            guild = ServerSettings()
            guild.id = this.id
            guild.name = this.name
            guild.realname = RealnameSetting.OFF
            guild.iconId = this.iconId
            guild.save()
            val musicSettings = MusicSettings()
            musicSettings.id = this.id
            musicSettings.save()
            unlock()
            sync()
            return
        }
        loadSettings()

        // Load role clearance sync because those are important
        clearances.clear()
        Model.where(RoleClearance::class.java, "server_id", this.id).get().forEach {
            clearances[it.roleId] = it.permission
        }

        // Load the rest of this stuff async
        val future = Bot.scheduler.submit {

            if (this.selfMember.nickname != settings.botNick) {
                Bot.LOG.debug("Updating nickname to \"${settings.botNick}\"")
                this.controller.setNickname(this.selfMember,
                        if (settings.botNick?.isEmpty() == true) null else settings.botNick).queue()
            }

            if (settings.name != this.name) {
                Bot.LOG.debug("Name has changed on ${this.name}, updating")
                settings.name = this.name
                settings.save()
            }

            if (settings.iconId != this.iconId) {
                settings.iconId = this.iconId
                settings.save()
            }

            updateChannels()

            val roles = Model.where(me.mrkirby153.KirBot.database.models.guild.Role::class.java,
                    "server_id", this.id).get().toMutableList()

            // Update the existing roles
            val removedRoles = mutableListOf<String>()
            roles.forEach {
                if (it.role != null) {
                    it.updateRole()
                } else {
                    it.delete()
                }
            }


            val rolesToAdd = mutableListOf<Role>()
            rolesToAdd.addAll(this.roles.filter { it.id !in roles.map { it.id } })

            Bot.LOG.debug("Adding roles ${rolesToAdd.map { it.id }}")
            Bot.LOG.debug("Removing roles $removedRoles")
            roles.removeIf { it.id in removedRoles }

            rolesToAdd.forEach {
                val role = me.mrkirby153.KirBot.database.models.guild.Role()
                role.role = it
                role.guild = this
                role.permissions = it.permissionsRaw
                role.save()
            }

            RealnameHandler(this).update(false)

            val groups = Model.where(Group::class.java, "server_id", this.id).get()

            groups.forEach { g ->
                if (g.role != null) {
                    g.members.mapNotNull {
                        val u = it.user
                        if (u == null)
                            null
                        else
                            this.getMemberById(u.id)
                    }.filter { g.role !in it.roles }.forEach { u ->
                        this.controller.addRolesToMember(u, g.role).queue()
                    }
                } else {
                    Bot.LOG.debug("Role was deleted. Removing from the database")
                    g.members.forEach { it.delete() }
                    g.delete()
                    return@forEach
                }
                this.members.filter { g.role !in it.roles }.filter { it.user.id !in g.members.map { it.id } }.forEach {
                    this.controller.removeRolesFromMember(it, g.role).queue()
                }
            }

            var members = Model.where(GuildMember::class.java, "server_id", this.id).get()

            val toDelete = mutableListOf<GuildMember>()
            val currentMembers = this.members.map { it.user.id }

            toDelete.addAll(members.filter { it.user?.id !in currentMembers })

            if (!guild.persistence) {
                Bot.LOG.debug("Deleting ${toDelete.map { it.user?.id }}")
                toDelete.forEach {
                    it.delete()
                }
            }

            val newMembers = this.members.filter { it.user.id !in members.map { it.user?.id } }

            Bot.LOG.debug("Adding ${newMembers.map { it.user.id }}")
            newMembers.forEach {
                val member = GuildMember()
                member.id = idGenerator.generate(10)
                member.user = it.user
                member.serverId = this.id
                member.nick = it.nickname
                member.save()
            }

            members.forEach {
                it.user = Bot.shardManager.getUser(it.userId)
                it.nick = it.user?.getMember(this)?.nickname
                it.save()
            }

            members = Model.where(GuildMember::class.java, "server_id", this.id).get()

            members.forEach { m ->
                val toRemove = mutableListOf<GuildMemberRole>()
                val toAdd = mutableListOf<Role>()
                if (m.user == null) {
                    Bot.LOG.debug("Removing ${m.userId} as they no longer exist")
                    m.delete()
                    return@forEach
                }

                val member = getMemberById(m.user!!.id) ?: return@forEach
                val current = member.roles

                toRemove.addAll(m.roles.filter { it.role?.id !in current.map { it.id } })
                toAdd.addAll(current.filter { it.id !in m.roles.map { it.role?.id } })

                if (toAdd.isNotEmpty()) {
                    Bot.LOG.debug("Adding roles $toAdd to ${member.user}")
                }
                if (toRemove.isNotEmpty())
                    Bot.LOG.debug(
                            "Removing roles ${toRemove.map { it.role?.id }} from ${member.user}")

                toAdd.forEach {
                    val memberRole = GuildMemberRole()
                    memberRole.id = keyGen.generate()
                    memberRole.server = this
                    memberRole.user = m.user
                    memberRole.role = it
                    memberRole.save()
                }
                toRemove.forEach { it.delete() }
            }
            Infractions.importFromBanlist(this)
            unlock()
            if (!ready)
                Bot.LOG.debug("Guild ${this} is ready!")
            ready = true
        }

        if (waitFor) {
            Bot.LOG.debug("Waiting for sync to complete")
            future.get()
            Bot.LOG.debug("Sync complete!")
        }
    }

    fun saveData() {
        Bot.LOG.debug("Saving data for $guild")
        val json = this.extraData.toString()
        ModuleManager[Redis::class.java].getConnection().use {
            it.set("data:${this.id}", json)
        }
    }

    fun loadData() {
        Bot.LOG.debug("Loading data for $guild")
        if (dataFile.exists()) {
            Bot.LOG.debug("Data file exists, migrating to redis")
            dataFile.inputStream().use {
                this.extraData = JSONObject(JSONTokener(it))
                ModuleManager[Redis::class.java].getConnection().use {
                    it.set("data:${this.id}", this.extraData.toString())
                }
            }
            Bot.LOG.debug("Migration complete, deleting old file")
            if (!dataFile.delete())
                dataFile.deleteOnExit()
        } else {
            ModuleManager[Redis::class.java].getConnection().use {
                this.extraData = JSONObject(JSONTokener(it.get("data:${this.id}") ?: "{}"))
            }
        }
    }

    fun getClearance(user: User): Int {
        val member = guild.getMember(user) ?: return 0
        return getClearance(member)
    }

    fun getClearance(member: Member): Int {
        lock()
        try {
            val redis = ModuleManager.getLoadedModule(Redis::class.java)
            redis?.getConnection()?.use { jedis ->
                if (jedis.sismember("admins", member.user.id)) {
                    unlock()
                    return CLEARANCE_GLOBAL_ADMIN
                }
            }
        } catch (e: Exception) {
            // Ignore
            unlock()
        }
        if (member.isOwner) {
            unlock()
            return CLEARANCE_ADMIN
        }
        var clearance = 0
        member.roles.forEach { role ->
            val roleClearance = this.clearances[role.id] ?: return@forEach
            if (clearance < roleClearance)
                clearance = roleClearance
        }
        unlock()
        return clearance
    }

    private fun updateChannels() {
        Bot.LOG.debug(
                "Updating channels on ${this.name} (${this.id})") // TODO 1/16/18: Update the name of the channel
        val channels = Model.where(Channel::class.java, "server", this.id).get().toMutableList()

        val removedChannels = mutableListOf<Channel>()
        channels.forEach {
            if (it.channel != null) {
                Bot.LOG.debug("Updating ${it.id} (${it.name})")
                it.updateChannel()
            } else {
                Bot.LOG.debug("Channel ${it.id} has been removed!")
                removedChannels.add(it)
            }
        }

        Bot.LOG.debug("Removing channels $removedChannels")

        removedChannels.forEach(Model::delete)
        channels.removeIf { it.id in removedChannels.map { it.id } }

        val registering = mutableListOf<net.dv8tion.jda.core.entities.Channel>()
        registering.addAll(
                guild.textChannels.map { it as net.dv8tion.jda.core.entities.Channel }.filter { it.id !in channels.map { it.id } })
        registering.addAll(
                guild.voiceChannels.map { it as net.dv8tion.jda.core.entities.Channel }.filter { it.id !in channels.map { it.id } })

        Bot.LOG.debug("Adding channels: $registering")
        registering.forEach {
            val channel = Channel()
            channel.channel = it
            channel.guild = this
            channel.updateChannel()
        }
    }

    fun syncSeenUsers() {
        Bot.LOG.debug("Syncing users")
        lock()
        val userIds = DB.getFirstColumnValues<String>("SELECT id FROM seen_users")
        val newMembers = this.members.filter { it.user.id !in userIds }
        Bot.LOG.debug("Found ${newMembers.size} new members")

        newMembers.map { it.user }.forEach { user ->
            val m = DiscordUser()
            m.id = user.id
            m.username = user.name
            m.discriminator = user.discriminator.toInt()
            m.create()
        }

        // Update usernames for people on the guild
        val results = DB.getResults(
                "SELECT `id`, `username`, `discriminator` FROM seen_users WHERE `id` IN (${this.members.map { it.user.id }.joinToString(
                        ",") { "'$it'" }})")
        val members = mutableMapOf<String, Member>()
        this.members.forEach {
            members[it.user.id] = it
        }
        results.forEach { row ->
            val member = members[row.getString("id")] ?: return@forEach
            if (member.user.name != row.getString(
                            "username") || member.user.discriminator.toInt() != row.getInt(
                            "discriminator")) {
                Bot.LOG.debug("Username for ${member.user.id} changed. Updating!")
                DB.executeUpdate(
                        "UPDATE `seen_users` SET `username` = ?, `discriminator` = ? WHERE id = ?",
                        member.user.name, member.user.discriminator, member.user.id)
            }
        }
        unlock()
    }

    fun createSelfrole(id: String) {
        if (id !in getSelfroles()) {
            this.extraData.append("selfroles", id)
            saveData()
        }
    }

    fun removeSelfrole(id: String) {
        this.extraData.put("selfroles", getSelfroles().toMutableList().apply { remove(id) })
        saveData()
    }

    fun getSelfroles(): List<String> {
        return this.extraData.optJSONArray("selfroles")?.toTypedArray(String::class.java)
                ?: listOf()
    }

    fun matchRole(query: String): Role? {
        // Check ID
        if (query.matches(Regex("\\d{17,18}"))) {
            return getRoleById(query) ?: null
        }
        // Try exact matches
        val exactMatches = roles.filter {
            it.name.toLowerCase().replace(" ", "") == query.replace(" ", "").toLowerCase()
        }
        if (exactMatches.isNotEmpty()) {
            if (exactMatches.size > 1)
                throw TooManyRolesException()
            return exactMatches.first()
        } else {
            val fuzzyRated = mutableMapOf<Role, Int>()
            roles.forEach { role ->
                fuzzyRated[role] = FuzzySearch.partialRatio(query.toLowerCase(),
                        role.name.toLowerCase().replace(" ", ""))

            }
            if (fuzzyRated.isEmpty())
                return null
            val entries = fuzzyRated.entries.sortedBy { it.value }.reversed().filter { it.value > 40 }
            val first = entries.first()
            return when {
                entries.size == 1 -> first.key
                first.value - entries[1].value > 20 -> first.key
                else -> throw TooManyRolesException()
            }
        }
    }

    override fun toString(): String {
        return "KirBotGuild(${this.name} - ${this.id})"
    }

    class TooManyRolesException : Exception()

    companion object {
        private val guilds = mutableMapOf<String, KirBotGuild>()
        private lateinit var leakDetectorThread: Thread
        private val idGenerator = IdGenerator(IdGenerator.ALPHA + IdGenerator.NUMBERS)

        var leakThreshold = 1000

        operator fun get(guild: Guild): KirBotGuild {
            synchronized(guilds) {
                return guilds.computeIfAbsent(guild.id, { KirBotGuild(guild) })
            }
        }

        fun remove(guild: Guild) {
            synchronized(guilds) {
                remove(guild.id)
            }
        }

        fun remove(id: String) {
            synchronized(guilds) {
                guilds.remove(id)
            }
        }

        fun startLeakMonitor() {
            leakDetectorThread = Thread {
                Bot.LOG.debug("Starting Guild lock leak monitor")
                while (true) {
                    guilds.values.forEach { it.checkLeak() }
                    Thread.sleep(10)
                }
            }.apply {
                name = "LeakDetector"
                isDaemon = true
            }
            leakDetectorThread.start()
        }
    }
}

