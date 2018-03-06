package me.mrkirby153.KirBot.server

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Channel
import me.mrkirby153.KirBot.database.models.CustomCommand
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.group.Group
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
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.kcutils.child
import me.mrkirby153.kcutils.mkdirIfNotExist
import me.mrkirby153.kcutils.utils.IdGenerator
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import org.json.JSONObject
import org.json.JSONTokener

class KirBotGuild(val guild: Guild) : Guild by guild {

    init {
        Bot.LOG.debug("Constructing KirBotGuild for $guild")
    }

    lateinit var settings: ServerSettings
    lateinit var customCommands: MutableList<CustomCommand>

    val isReady: Boolean
        get() = isSynced && settingsLoaded

    var isSynced = false
    var settingsLoaded = false

    val musicManager = MusicManager(this)
    val logManager = LogManager(this)

    var extraData = JSONObject()
    private val dataFile = Bot.files.data.child("servers").mkdirIfNotExist().child("${this.id}.json")

    fun loadSettings() {
        Bot.LOG.debug("Loading settings for ${this}")

        settings = Model.first(ServerSettings::class.java,
                Pair("id", this.id)) ?: throw IllegalStateException(
                "Attempting to load settings for a guild that doesn't exist")

        customCommands = Model.get(CustomCommand::class.java,
                Pair("server", this.id)).toMutableList()

        loadData()

        settingsLoaded = true
    }

    fun onPart() {
        dataFile.delete()
        ModuleManager[Redis::class.java].getConnection().use {
            it.del("data:${this.id}")
        }
    }

    fun sync() {
        val keyGen = IdGenerator(IdGenerator.ALPHA + IdGenerator.NUMBERS)
        Bot.LOG.debug("Syncing guild ${this.id}")
        var guild = Model.first(ServerSettings::class.java, this.id)

        if (guild == null) {
            Bot.LOG.debug("Guild does not exist... Creating")
            guild = ServerSettings()
            guild.id = this.id
            guild.name = this.name
            guild.realname = RealnameSetting.OFF
            guild.save()
            val musicSettings = MusicSettings()
            musicSettings.id = this.id
            musicSettings.save()
            sync()
            return
        }

        if (!settingsLoaded)
            loadSettings()


        if (this.selfMember.nickname != settings.botNick) {
            Bot.LOG.debug("Updating nickname to \"${settings.botNick}\"")
            this.controller.setNickname(this.selfMember,
                    if (settings.botNick?.isEmpty() == true) null else settings.botNick).queue()
        }

        if (settings.name != this.name) {
            Bot.LOG.debug("Name has changed on ${this.name}, updating")
            guild.name = this.name
            guild.save()
        }

        updateChannels()

        val roles = Model.get(me.mrkirby153.KirBot.database.models.guild.Role::class.java,
                Pair("server_id", this.id))

        roles.forEach {
            if (it.role != null) {
                it.permissions = it.role!!.permissionsRaw
                it.save()
            }
        }

        val rolesToAdd = mutableListOf<Role>()
        val rolesToRemove = mutableListOf<me.mrkirby153.KirBot.database.models.guild.Role>()

        rolesToRemove.addAll(roles)
        rolesToRemove.removeIf { role -> role.id in this.roles.map { it.id } }
        rolesToAdd.addAll(this.roles.filter { it.id !in roles.map { it.id } })

        Bot.LOG.debug("Adding roles ${rolesToAdd.map { it.id }}")
        Bot.LOG.debug("Removing roles ${rolesToRemove.map { it.id }}")

        rolesToAdd.forEach {
            val role = me.mrkirby153.KirBot.database.models.guild.Role()
            role.role = it
            role.guild = this
            role.permissions = it.permissionsRaw
            role.save()
        }

        rolesToRemove.forEach(Model::delete)

        RealnameHandler(this).update()

        val groups = Model.get(Group::class.java, Pair("server_id", this.id))

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

        var members = Model.get(GuildMember::class.java, Pair("server_id", this.id))

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
            member.id = Model.randomId()
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

        members = Model.get(GuildMember::class.java, Pair("server_id", this.id))

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
                Bot.LOG.debug("Removing roles ${toRemove.map { it.role?.id }} from ${member.user}")

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
        isSynced = true
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
        if(dataFile.exists()){
            Bot.LOG.debug("Data file exists, migrating to redis")
            dataFile.inputStream().use {
                this.extraData = JSONObject(JSONTokener(it))
                ModuleManager[Redis::class.java].getConnection().use {
                    it.set("data:${this.id}", this.extraData.toString())
                }
            }
            Bot.LOG.debug("Migration complete, deleting old file")
            if(!dataFile.delete())
                dataFile.deleteOnExit()
        } else {
            ModuleManager[Redis::class.java].getConnection().use {
                this.extraData = JSONObject(JSONTokener(it.get("data:${this.id}") ?: "{}"))
            }
        }
    }

    private fun updateChannels() {
        Bot.LOG.debug(
                "Updating channels on ${this.name} (${this.id})") // TODO 1/16/18: Update the name of the channel
        val channels = Model.get(Channel::class.java, Pair("server", this.id))

        val registering = mutableListOf<net.dv8tion.jda.core.entities.Channel>()
        val unregistering = mutableListOf<Channel>()

        unregistering.addAll(channels)
        unregistering.removeIf { chan -> chan.id in guild.textChannels.map { it.id } }
        unregistering.removeIf { chan -> chan.id in guild.voiceChannels.map { it.id } }

        registering.addAll(guild.textChannels.filter { it.id !in channels.map { it.id } })
        registering.addAll(guild.voiceChannels.filter { it.id !in channels.map { it.id } })

        Bot.LOG.debug("Registering channels $registering")
        Bot.LOG.debug("Removing channels $unregistering")

        registering.forEach {
            val channel = Channel()
            channel.id = it.id
            channel.type = if (it is TextChannel) Channel.Type.TEXT else if (it is VoiceChannel) Channel.Type.VOICE else Channel.Type.UNKNOWN
            channel.name = it.name
            channel.guild = this
            channel.hidden = false
            channel.save()
        }

        unregistering.forEach(Model::delete)
    }

    override fun toString(): String {
        return "KirBotGuild(${this.name} - ${this.id})"
    }

    companion object {
        private val guilds = mutableMapOf<String, KirBotGuild>()

        operator fun get(guild: Guild): KirBotGuild {
            return guilds.computeIfAbsent(guild.id, { KirBotGuild(guild) })
        }

        fun remove(guild: Guild) {
            remove(guild.id)
        }

        fun remove(id: String) {
            guilds.remove(id)
        }
    }
}

