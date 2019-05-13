package me.mrkirby153.KirBot.server

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.SoftDeletingModel
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.MessageConcurrencyManager
import me.mrkirby153.KirBot.database.models.Channel
import me.mrkirby153.KirBot.database.models.CustomCommand
import me.mrkirby153.KirBot.database.models.DiscordUser
import me.mrkirby153.KirBot.database.models.RoleClearance
import me.mrkirby153.KirBot.database.models.guild.CommandAlias
import me.mrkirby153.KirBot.database.models.guild.DiscordGuild
import me.mrkirby153.KirBot.database.models.guild.GuildMember
import me.mrkirby153.KirBot.database.models.guild.GuildMemberRole
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.SettingsRepository
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.fuzzyMatch
import me.mrkirby153.KirBot.utils.toTypedArray
import me.mrkirby153.kcutils.child
import me.mrkirby153.kcutils.mkdirIfNotExist
import me.mrkirby153.kcutils.use
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.Future
import java.util.stream.Collectors

class KirBotGuild(val guild: Guild) : Guild by guild {

    init {
        Bot.LOG.debug("Constructing KirBotGuild for $guild")
    }

    lateinit var customCommands: MutableList<CustomCommand>
    lateinit var commandAliases: MutableList<CommandAlias>
    val clearances: MutableMap<String, Int> = mutableMapOf()


    val logManager = LogManager(this)

    val discordGuild: DiscordGuild by lazy {
        Model.where(DiscordGuild::class.java, "id", this.id).first()
    }

    var extraData = JSONObject()
    private val dataFile = Bot.files.data.child("servers").mkdirIfNotExist().child(
            "${this.id}.json")

    private val visibleChannelCache = mutableMapOf<String, Boolean>()

    var ready = false

    private val runningTasks = mutableListOf<Future<*>>()

    /**
     * Load guild-specific settings
     */
    fun loadSettings() {
        Bot.LOG.debug("Loading settings for ${this}")
        // Load settings
        val dg = SoftDeletingModel.withTrashed(DiscordGuild::class.java).where("id",
                this.id).first() ?: DiscordGuild(this)
        if (dg.isTrashed) {
            Bot.LOG.debug("Restoring trashed guild $dg")
            dg.restore()
        }
        dg.id = this.id
        dg.name = this.name
        dg.iconId = this.iconId
        dg.owner = this.ownerId
        dg.save()

        customCommands = Model.where(CustomCommand::class.java,
                "server", this.id).get().toMutableList()
        commandAliases = Model.where(CommandAlias::class.java, "server_id",
                this.id).get().toMutableList()


        logManager.reloadLogChannels()

        loadData()

        // Load clearances
        clearances.clear()
        Model.where(RoleClearance::class.java, "server_id", this.id).get().forEach {
            clearances[it.roleId] = it.permission
        }
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
            val botNick = SettingsRepository.get(guild, "bot_nick")
            if (this.selfMember.nickname != botNick) {
                this.controller.setNickname(this.selfMember,
                        if (botNick?.isEmpty() == true) null else botNick).queue()
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

            this.ready = true
            Bot.shardManager.getEventManager(this).onGuildReady(this)
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
        if (member.user.id in overriddenUsers) {
            return Integer.MAX_VALUE
        }
        if (member.isOwner) {
            return CLEARANCE_ADMIN
        }
        var clearance = 0
        member.roles.forEach { role ->
            val roleClearance = this.clearances[role.id] ?: return@forEach
            if (clearance < roleClearance)
                clearance = roleClearance
        }
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
            val toInsert = mutableListOf<Message>()
            val toUpdate = mutableListOf<Message>()
            history.forEach { message ->
                val stored = storedMessages.firstOrNull { it.id == message.id }
                if (stored == null) {
                    toInsert.add(message)
                    new++
                } else {
                    if (stored.message != message.contentRaw) {
                        toUpdate.add(message)
                    }
                }
            }
            if (toInsert.isNotEmpty())
                MessageConcurrencyManager.insert(*toInsert.toTypedArray())
            if (toUpdate.isNotEmpty())
                MessageConcurrencyManager.update(*toUpdate.toTypedArray())
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
        Bot.LOG.debug(
                "Removed $count completed tasks on $this. (${this.runningTasks.size} still pending)")
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

        fun setOverride(user: User, value: Boolean) {
            if (value) {
                overriddenUsers.add(user.id)
            } else {
                overriddenUsers.remove(user.id)
            }
        }
    }
}

