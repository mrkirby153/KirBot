package me.mrkirby153.KirBot.server

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.SoftDeletingModel
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Channel
import me.mrkirby153.KirBot.database.models.CustomCommand
import me.mrkirby153.KirBot.database.models.DiscordUser
import me.mrkirby153.KirBot.database.models.RoleClearance
import me.mrkirby153.KirBot.database.models.guild.CommandAlias
import me.mrkirby153.KirBot.database.models.guild.DiscordGuild
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.fuzzyMatch
import me.mrkirby153.KirBot.utils.settings.GuildSettings
import me.mrkirby153.KirBot.utils.toTypedArray
import me.mrkirby153.kcutils.child
import me.mrkirby153.kcutils.mkdirIfNotExist
import me.mrkirby153.kcutils.use
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import org.json.JSONObject
import org.json.JSONTokener
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Future
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Supplier

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

    private var extraData = JSONObject()
    private var extraDataLock = ReentrantReadWriteLock()

    private val dataFile = Bot.files.data.child("servers").mkdirIfNotExist().child(
            "${this.id}.json")

    val ready
        get() = readyFuture.isDone

    private val runningTasks: CopyOnWriteArrayList<Future<*>> = CopyOnWriteArrayList()

    private lateinit var readyFuture: CompletableFuture<Void>

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

    fun updateRoleClearance(role: String, clearance: Int) {
        clearances[role] = clearance
    }

    fun deleteRoleClearance(role: String) {
        clearances.remove(role)
    }

    fun sync() {
        Bot.LOG.debug("STARTING SYNC FOR $this")
        loadSettings()

        // Load the rest asynchronously
        val nickFuture = runAsyncTask {
            val botNick = GuildSettings.botNick.nullableGet(this)
            if (this.selfMember.nickname != botNick) {
                this.selfMember.modifyNickname(
                        if (botNick?.isEmpty() == true) null else botNick).queue()
            }
        }

        val channelFuture = runAsyncTask {
            Bot.LOG.debug("Updating channels on $this")
            val channels = this.textChannels.map { it as GuildChannel }.union(
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

        val roleFuture = runAsyncTask {
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

        val seenUsersFuture = syncSeenUsers()

        readyFuture = CompletableFuture.allOf(nickFuture, channelFuture, roleFuture,
                seenUsersFuture)

        readyFuture.thenRun {
            Bot.LOG.info("Guild ${guild.id} is ready")
            removeCompletedTasks()
        }.exceptionally { ex ->
            Bot.LOG.error("Guild ${guild.id} did not initialize correctly", ex)
            removeCompletedTasks()
            return@exceptionally null
        }
    }

    fun runWithExtraData(writable: Boolean = false, runnable: (JSONObject) -> Unit) {
        if (writable) {
            extraDataLock.writeLock().lock()
        } else {
            extraDataLock.readLock().lock()
        }
        try {
            val existing = extraData.toString()
            runnable.invoke(extraData)
            if (extraData.toString() != existing) {
                if (!writable) {
                    Bot.LOG.warn(
                            "Extra data on $this has changed while not open for writing. This will be ignored")
                } else {
                    Bot.LOG.debug("Extra data on $this has been modified. Saving")
                    saveData()
                }
            }
        } finally {
            if (writable) {
                extraDataLock.writeLock().unlock()
            } else {
                extraDataLock.readLock().unlock()
            }
        }
    }

    private fun saveData() {
        Bot.LOG.debug("Saving data for $guild")
        val json = this.extraData.toString()
        ModuleManager[Redis::class.java].getConnection().use {
            it.set("data:${this.id}", json)
        }
    }

    private fun loadData() {
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

    fun syncSeenUsers(): CompletableFuture<Unit> {
        return runAsyncTask {
            Bot.LOG.debug("Syncing seen users on $this")
            val guildUsers = Model.query(DiscordUser::class.java).whereIn("id",
                    members.map { it.id }.toTypedArray()).get()
            guildUsers.forEach(DiscordUser::updateUser)
            val newMembers = members.filter { it.id !in guildUsers.map { it.id } }
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
            return getRoleById(query)
        }
        return fuzzyMatch(this.roles, query.replace(" ", "").toLowerCase(),
                { it.name.replace(" ", "").toLowerCase() })
    }

    fun <T> runAsyncTask(task: () -> T): CompletableFuture<T> {
        removeCompletedTasks()
        val future = CompletableFuture.supplyAsync(Supplier {
            task.invoke()
        }, Bot.scheduler)
        runningTasks.add(future)
        return future
    }

    fun cancelAllTasks(interrupt: Boolean = true) {
        removeCompletedTasks()
        this.runningTasks.forEach { it.cancel(interrupt) }
        this.runningTasks.clear()
    }

    private fun removeCompletedTasks() {
        val finishedTasks = runningTasks.filter { it.isDone }
        runningTasks.removeAll(finishedTasks)
        Bot.LOG.debug(
                "[$this] Removed ${finishedTasks.size} completed tasks on $this. (${this.runningTasks.size} still pending)")
    }

    override fun toString(): String {
        return "KirBotGuild(${this.name} - ${this.id})"
    }

    companion object {
        private val guilds: ConcurrentHashMap<String, KirBotGuild> = ConcurrentHashMap()

        private val overriddenUsers = mutableSetOf<String>()

        operator fun get(guild: Guild): KirBotGuild {
            return guilds.computeIfAbsent(guild.id) { KirBotGuild(guild) }
        }

        fun remove(guild: Guild) {
            remove(guild.id)
        }

        fun remove(id: String) {
            guilds.remove(id)
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

