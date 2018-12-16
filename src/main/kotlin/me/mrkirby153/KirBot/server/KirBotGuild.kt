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
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.AntiRaid
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
import java.util.concurrent.Future
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

    private val runningTasks = mutableListOf<Future<*>>()

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

    /**
     * Load guild-specific settings
     */
    fun loadSettings() {
        Bot.LOG.debug("Loading settings for ${this}")
        // Load settings
        settings = SoftDeletingModel.withTrashed(ServerSettings::class.java).where("id",
                this.id).first() ?: ServerSettings()
        if (settings.isTrashed) {
            Bot.LOG.debug("Restoring trashed guild settings on $this")
            settings.restore()
        }
        settings.id = this.id
        settings.name = this.name
        settings.iconId = this.iconId
        settings.save()

        customCommands = Model.where(CustomCommand::class.java,
                "server", this.id).get().toMutableList()
        commandAliases = Model.where(CommandAlias::class.java, "server_id",
                this.id).get().toMutableList()

        // Ensure music settings exist
        val musicSettings = Model.where(MusicSettings::class.java, "id", this.id).first()
                ?: MusicSettings(this)
        musicSettings.save()

        logManager.reloadLogChannels()

        // Purge anti-raid cache
        ModuleManager.getLoadedModule(AntiRaid::class.java)?.raidSettingsCache?.invalidate(this.id)

        loadData()

        // Load clearances
        clearances.clear()
        Model.where(RoleClearance::class.java, "server_id", this.id).get().forEach {
            clearances[it.roleId] = it.permission
        }
        // The guild is ready to process events even though stuff may still be loading
        this.ready = true
    }

    fun onPart() {
        cancelAllTasks(true)
        dataFile.delete()
        ModuleManager[Redis::class.java].getConnection().use {
            it.del("data:${this.id}")
        }
        KirBotGuild.remove(this)
    }

    fun sync() {
        Bot.LOG.debug("STARTING SYNC FOR $this")
        cacheVisibilities(false)
        loadSettings()

        runAsyncTask {
            if (this.selfMember.nickname != settings.botNick) {
                this.controller.setNickname(this.selfMember,
                        if (settings.botNick?.isEmpty() == true) null else settings.botNick).queue()
            }
        }

        runAsyncTask {
            Bot.LOG.debug("Updating channels on $this")
            val channels = this.textChannels.map { it as net.dv8tion.jda.core.entities.Channel }.union(
                    this.voiceChannels)
            Model.query(Channel::class.java).whereNotIn("id",
                    channels.map { it.id }.toTypedArray()).where("server", this.id).delete()
            val storedChannels = Model.where(Channel::class.java, "server", this.id).get()
            storedChannels.forEach(Channel::updateChannel) // Update existing channels
            channels.filter { it.id !in storedChannels.map { it.id } }.forEach {
                Bot.LOG.debug("adding channel: $it")
                Channel(it).save()
            }
        }

        runAsyncTask {
            if (this.selfMember.hasPermission(Permission.NICKNAME_MANAGE))
                RealnameHandler(this).update(false)
        }

        runAsyncTask {
            Model.query(me.mrkirby153.KirBot.database.models.guild.Role::class.java).whereNotIn(
                    "id",
                    this.roles.map { it.id }.toTypedArray()
            ).where("server_id", this.id).delete()
            val storedRoles = Model.where(
                    me.mrkirby153.KirBot.database.models.guild.Role::class.java, "server_id",
                    this.id).get()
            storedRoles.forEach(me.mrkirby153.KirBot.database.models.guild.Role::updateRole)
            this.roles.filter { it.id !in storedRoles.map { it.id } }.forEach {
                me.mrkirby153.KirBot.database.models.guild.Role(it).save()
            }
        }

        runAsyncTask {
            // Update guild members & their roles
            val members = Model.where(GuildMember::class.java, "server_id", this.id).get()
            members.forEach(GuildMember::updateMember)
            val memberRoles = mutableMapOf<String, MutableList<GuildMemberRole>>()
            Model.where(GuildMemberRole::class.java, "server_id", this.id).get().forEach {
                val r = memberRoles.getOrPut(it.id) { mutableListOf() }
                r.add(it)
            }
            this.members.forEach { member ->
                val currentRoles = memberRoles[member.user.id] ?: return@forEach
                member.roles.filter { it.id !in currentRoles.map { it.id } }.forEach {
                    GuildMemberRole(member, it).save()
                }
                // Delete roles the user no longer has
                if (member.roles.isNotEmpty())
                    Model.where(GuildMemberRole::class.java, "user_id", member.user.id).where(
                            "server_id", member.guild.id).whereNotIn("role_id",
                            member.roles.map { it.id }.toTypedArray()).delete()
            }
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
        runAsyncTask {
            val users = Model.query(DiscordUser::class.java).get()
            users.forEach(DiscordUser::updateUser)
            val newMembers = this.members.filter { it.user.id !in users.map { it.id } }
            Bot.LOG.debug("Found ${newMembers.size} new members")
            newMembers.map { it.user }.forEach { user ->
                DiscordUser(user).save()
            }
        }
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

    fun runAsyncTask(task: () -> Unit) {
        removeCompletedTasks()
        val future = Bot.scheduler.submit(task)
        this.runningTasks.add(future)
    }

    fun hasAsyncTasksPending(): Boolean {
        removeCompletedTasks()
        return this.runningTasks.isNotEmpty()
    }

    fun cancelAllTasks(interrupt: Boolean = true) {
        removeCompletedTasks()
        this.runningTasks.forEach { it.cancel(interrupt) }
        this.runningTasks.clear()
    }

    private fun removeCompletedTasks() {
        val it = this.runningTasks.iterator()
        var count = 0
        while (it.hasNext()) {
            val f = it.next()
            if (f.isDone) {
                it.remove()
                count++
            }
        }
        Bot.LOG.debug("Removed $count completed tasks on $this")
    }

    override fun toString(): String {
        return "KirBotGuild(${this.name} - ${this.id})"
    }

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

