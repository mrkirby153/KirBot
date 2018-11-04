package me.mrkirby153.KirBot.server

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.SoftDeletingModel
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Channel
import me.mrkirby153.KirBot.database.models.CustomCommand
import me.mrkirby153.KirBot.database.models.DiscordUser
import me.mrkirby153.KirBot.database.models.RoleClearance
import me.mrkirby153.KirBot.database.models.guild.CommandAlias
import me.mrkirby153.KirBot.database.models.guild.GuildMember
import me.mrkirby153.KirBot.database.models.guild.GuildMemberRole
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.database.models.guild.MusicSettings
import me.mrkirby153.KirBot.database.models.guild.ServerSettings
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.realname.RealnameHandler
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.fuzzyMatch
import me.mrkirby153.KirBot.utils.toTypedArray
import me.mrkirby153.kcutils.child
import me.mrkirby153.kcutils.mkdirIfNotExist
import me.mrkirby153.kcutils.use
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.Semaphore
import java.util.stream.Collectors

class KirBotGuild(val guild: Guild) : Guild by guild {

    init {
        Bot.LOG.debug("Constructing KirBotGuild for $guild")
    }

    lateinit var settings: ServerSettings
    lateinit var customCommands: MutableList<CustomCommand>
    lateinit var commandAliases: MutableList<CommandAlias>
    val clearances: MutableMap<String, Int> = mutableMapOf()


    val logManager = LogManager(this)

    var extraData = JSONObject()
    private val dataFile = Bot.files.data.child("servers").mkdirIfNotExist().child(
            "${this.id}.json")

    private val lock = Semaphore(1, true)
    private var lockedAt: Long = -1
    private var lockedThread: Thread? = null
    private var lockDump: Array<StackTraceElement> = arrayOf()
    private var lockDumped = false

    private val visibleChannelCache = mutableMapOf<String, Boolean>()

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
        Bot.LOG.debug("STARTING SYNC FOR $this")
        cacheVisibilities(false)
        settings = SoftDeletingModel.withTrashed(ServerSettings::class.java).where("id",
                this.id).first() ?: ServerSettings()
        settings.deletedAt = null
        settings.id = this.id
        settings.name = this.name
        settings.iconId = this.iconId
        settings.save()

        val musicSettings = Model.where(MusicSettings::class.java, "id", this.id).first()
                ?: MusicSettings(this)
        musicSettings.save()

        loadSettings()
        clearances.clear()
        Model.where(RoleClearance::class.java, "server_id", this.id).get().forEach {
            clearances[it.roleId] = it.permission
        }

        val future = Bot.scheduler.submit {
            if (this.selfMember.nickname != settings.botNick) {
                Bot.LOG.debug("Updating nickname to \"${settings.botNick}\"")
                this.controller.setNickname(this.selfMember,
                        if (settings.botNick?.isEmpty() == true) null else settings.botNick).queue()
            }

            // Update channels
            Bot.LOG.debug("Updating channels on ${this.name} (${this.id})")
            val channels = this.textChannels.map { it as net.dv8tion.jda.core.entities.Channel }.union(
                    this.voiceChannels)
            Model.query(Channel::class.java).whereNotIn("id",
                    channels.map { it.id }.toTypedArray()).where("server", this.id).delete()
            val storedChannels = Model.where(Channel::class.java, "server", this.id).get()
            storedChannels.forEach(Channel::updateChannel)
            val storedChannelIds = storedChannels.map { it.id }

            channels.filter { it.id !in storedChannelIds }.forEach {
                Bot.LOG.debug("Adding channel: $it")
                Channel(it).save()
            }

            // Update Roles
            Model.query(me.mrkirby153.KirBot.database.models.guild.Role::class.java).whereNotIn(
                    "id",
                    this.roles.map { it.id }.toTypedArray()).where("server_id", this.id).delete()
            val storedRoles = Model.where(
                    me.mrkirby153.KirBot.database.models.guild.Role::class.java, "server_id",
                    this.id).get()
            storedRoles.forEach(me.mrkirby153.KirBot.database.models.guild.Role::updateRole)
            val roleIds = storedRoles.map { it.id }
            this.roles.filter { it.id !in roleIds }.forEach {
                me.mrkirby153.KirBot.database.models.guild.Role(it).save()
            }

            if (this.selfMember.hasPermission(Permission.NICKNAME_MANAGE))
                RealnameHandler(this).update(false)

            // Update guild members
            val members = Model.where(GuildMember::class.java, "server_id", this.id).get()
            members.forEach(GuildMember::updateMember)
            this.members.filter { it.user.id !in members.map { it.userId } }.forEach { m ->
                GuildMember(m).save()
            }

            val memberRoles = Model.where(GuildMemberRole::class.java, "server_id", this.id).get()
            this.members.forEach { member ->
                member.roles.filter { it.id !in memberRoles.filter { it.user?.id == member.user.id }.mapNotNull { it.role?.id } }.forEach { r ->
                    GuildMemberRole(member, r).save()
                }
                if (member.roles.isNotEmpty())
                    Model.where(GuildMemberRole::class.java, "user_id", member.user.id).where(
                            "server_id", member.guild.id).whereNotIn("role_id",
                            member.roles.map { it.id }.toTypedArray()).delete()
            }
            Infractions.importFromBanlist(this)
            if (!ready)
                Bot.LOG.debug("Guild $this is ready")
            ready = true
            Bot.LOG.debug("Sync complete!")
        }
        if (waitFor) {
            future.get()
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
        if (member.user.id in overriddenUsers) {
            unlock()
            return Integer.MAX_VALUE
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

    fun syncSeenUsers() {
        Bot.LOG.debug("Syncing users")
        lock()
        val users = Model.get(DiscordUser::class.java)
        users.forEach(DiscordUser::updateUser)
        val newMembers = this.members.filter { it.user.id !in users.map { it.id } }
        Bot.LOG.debug("Found ${newMembers.size} new members")
        newMembers.map { it.user }.forEach { user ->
            DiscordUser(user).save()
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
        return fuzzyMatch(this.roles, query.replace(" ", "").toLowerCase(),
                { it.name.replace(" ", "").toLowerCase() })
    }

    fun dispatchBackfill() {
        Bot.scheduler.submit {
            backfillChannels()
        }
    }

    fun cacheVisibilities(backfill: Boolean = true) {
        val newChannels = mutableListOf<TextChannel>()
        this.textChannels.forEach { c ->
            val canView = c.checkPermissions(Permission.VIEW_CHANNEL)
            if (visibleChannelCache[c.id] != canView && canView)
                newChannels.add(c)
            visibleChannelCache[c.id] = canView
        }
        if (backfill)
            if (newChannels.isNotEmpty()) {
                Bot.LOG.debug("Found ${newChannels.size} new channels, backfilling")
                Bot.scheduler.submit {
                    newChannels.forEach {
                        backfillChannels(it)
                    }
                }
            }
    }

    fun backfillChannels(channel: net.dv8tion.jda.core.entities.TextChannel? = null) {
        if (channel != null) {
            Bot.LOG.debug("Backfilling ${channel.name}")
            if (!channel.checkPermissions(Permission.MESSAGE_HISTORY)) {
                Bot.LOG.debug(
                        "Skipping backfill on ${channel.name} - No permission to read history")
                return
            }
            val history = channel.iterableHistory.stream().limit(500).collect(Collectors.toList())
            val storedMessages = if (history.isNotEmpty()) Model.query(
                    GuildMessage::class.java).whereIn("id",
                    history.map { it.id }.toTypedArray()).get() else listOf()
            var new = 0
            var updated = 0
            history.forEach { message ->
                val stored = storedMessages.firstOrNull { it.id == message.id }
                if (stored == null) {
                    GuildMessage(message).save()
                    new++
                } else {
                    if (stored.message != message.contentRaw) {
                        stored.message = message.contentRaw
                        stored.editCount++
                        updated++
                        stored.save()
                    }
                }
            }
            Bot.LOG.debug(
                    "Backfilled ${channel.name}: $new new, $updated updated out of ${history.size} total")
        } else {
            this.guild.textChannels.forEach { backfillChannels(it) }
        }
    }

    override fun toString(): String {
        return "KirBotGuild(${this.name} - ${this.id})"
    }

    class TooManyRolesException : Exception()

    companion object {
        private val guilds = mutableMapOf<String, KirBotGuild>()
        private lateinit var leakDetectorThread: Thread

        private val overriddenUsers = mutableSetOf<String>()

        var leakThreshold = 1000

        operator fun get(guild: Guild): KirBotGuild {
            synchronized(guilds) {
                return guilds.computeIfAbsent(guild.id) { KirBotGuild(guild) }
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

        fun setOverride(user: User, value: Boolean) {
            if (value) {
                overriddenUsers.add(user.id)
            } else {
                overriddenUsers.remove(user.id)
            }
        }
    }
}

