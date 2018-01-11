package me.mrkirby153.KirBot.server

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.api.ClearanceOverride
import me.mrkirby153.KirBot.database.api.GuildChannel
import me.mrkirby153.KirBot.database.api.GuildCommand
import me.mrkirby153.KirBot.database.api.GuildMember
import me.mrkirby153.KirBot.database.api.GuildRole
import me.mrkirby153.KirBot.database.api.GuildSettings
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.music.MusicManager
import me.mrkirby153.KirBot.realname.RealnameHandler
import me.mrkirby153.kcutils.child
import me.mrkirby153.kcutils.mkdirIfNotExist
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Guild
import org.json.JSONObject
import org.json.JSONTokener
import java.io.FileWriter

class KirBotGuild(val guild: Guild) : Guild by guild {

    init {
        Bot.LOG.debug("Constructing KirBotGuild for $guild")
    }

    lateinit var settings: GuildSettings
    lateinit var customCommands: MutableList<GuildCommand>
    lateinit var clearanceOverrides: MutableList<ClearanceOverride>

    val isReady: Boolean
        get() = isSynced && settingsLoaded

    var isSynced = false
    var settingsLoaded = false

    val musicManager = MusicManager(this)
    val logManager = LogManager(this)

    var extraData = JSONObject()
    private val dataFile = Bot.files.data.child("servers").mkdirIfNotExist().child(
            "${this.id}.json")

    private fun loadSettings() {
        Bot.LOG.debug("Loading settings for ${this}")

        settings = GuildSettings.get(this).get()

        customCommands = GuildCommand.getCommands(this).get().toMutableList()

        clearanceOverrides = ClearanceOverride.get(this).get()

        loadData()

        settingsLoaded = true
    }

    fun onPart() {
        dataFile.delete()
    }

    fun sync() {
        Bot.LOG.debug("Syncing guild ${this.id}")
        if (!PanelAPI.serverExists(this).get()) {
            PanelAPI.registerServer(this).get()
        }
        if (!settingsLoaded)
            loadSettings()


        if (this.selfMember.nickname != settings.nick) {
            Bot.LOG.debug("Updating nickname to \"${settings.nick}\"")
            this.controller.setNickname(this.selfMember,
                    if (settings.nick?.isEmpty() == true) null else settings.nick).queue()
        }

        if (settings.name != this.name) {
            Bot.LOG.debug("Name has changed on ${this.name}, updating")
            PanelAPI.setServerName(this).get()
            // We don't update our settings copy here, this probably won't cause any issues
        }

        updateChannels()

        val roles = PanelAPI.getRoles(this).get()

        val storedRoleIds = roles.map { it.id }

        roles.forEach {
            if (it.role != null) {
                val realPerms = it.role.permissionsRaw
                val perms = it.permissions
                if (realPerms != perms) {
                    Bot.LOG.debug("Permissions for role ${it.role.name} have changed. Updating")
                    it.update().get()
                }
            }
        }

        val rolesToAdd = mutableListOf<String>()
        val rolesToRemove = mutableListOf<String>()

        rolesToRemove.addAll(storedRoleIds)

        rolesToRemove.removeAll(this.roles.map { it.id })
        rolesToAdd.addAll(this.roles.map { it.id }.filter { it !in storedRoleIds })

        Bot.LOG.debug("Adding roles $rolesToAdd")
        Bot.LOG.debug("Removing roles $rolesToRemove")

        rolesToAdd.map { getRoleById(it) }.filter { it != null }.forEach {
            GuildRole.create(it).get()
        }
        rolesToRemove.forEach { GuildRole.delete(it).get() }

        RealnameHandler(this).update()

        val groups = PanelAPI.getGroups(this).get()
        groups.forEach { g ->
            if (g.role != null) {
                g.members.map {
                    this.getMemberById(it)
                }.filter { g.role !in it.roles }.forEach { u ->
                    this.controller.addRolesToMember(u, g.role).queue()
                }
            }
            this.members.filter { g.role !in it.roles }.filter { it.user.id !in g.members }.forEach {
                this.controller.removeRolesFromMember(it, g.role).queue()
            }
        }

        var members = PanelAPI.getMembers(this).get()
        val toDelete = mutableListOf<GuildMember>()
        val currentMembers = this.members.map { it.user.id }
        toDelete.addAll(members.filter { it.userId !in currentMembers })

        if (!settings.persistence) {
            Bot.LOG.debug("Deleting " + toDelete.map { it.userId })
            toDelete.forEach {
                it.delete().get()
            }
        }

        val newMembers = this.members.filter { it.user.id !in members.map { it.userId } }

        Bot.LOG.debug("Adding " + newMembers.map { it.user.id })

        newMembers.forEach { GuildMember.create(it).get() }
        members.filter { it.needsUpdate() }.forEach { it.update().get() }

        members = PanelAPI.getMembers(this).get() // Refresh the members

        members.forEach { m ->
            val toRemove = mutableListOf<String>()
            val toAdd = mutableListOf<String>()

            val member = getMemberById(m.userId) ?: return@forEach

            val current = member.roles.map { it.id }

            toRemove.addAll(m.roles.filter { it !in current })
            toAdd.addAll(current.filter { it !in m.roles })
            if (toAdd.isNotEmpty())
                Bot.LOG.debug("Adding roles $toAdd to ${member.user}")
            if (toRemove.isNotEmpty())
                Bot.LOG.debug("Removing roles $toRemove from ${member.user}")

            toAdd.forEach { m.addRole(it).execute() }
            toRemove.forEach { m.removeRole(it).execute() }
        }
    }

    fun saveData() {
        val json = this.extraData.toString()
        FileWriter(dataFile).use {
            it.write(json)
            it.flush()
        }
    }

    fun loadData() {
        dataFile.inputStream().use {
            this.extraData = JSONObject(JSONTokener(it))
        }
    }

    private fun updateChannels() {
        Bot.LOG.debug("Updating channels on ${this.name} (${this.id})")
        val c = PanelAPI.getChannels(this).get()
        val channels = mutableListOf<String>()
        channels.addAll(c.text.map { it.id })
        channels.addAll(c.voice.map { it.id })

        val registering = mutableListOf<String>()
        val unregistering = mutableListOf<String>()

        unregistering.addAll(channels)
        unregistering.removeAll(guild.textChannels.map { it.id })
        unregistering.removeAll(guild.voiceChannels.map { it.id })

        registering.addAll(guild.textChannels.filter { it.id !in channels }.map { it.id })
        registering.addAll(guild.voiceChannels.filter { it.id !in channels }.map { it.id })

        Bot.LOG.debug("Registering channels $registering")
        Bot.LOG.debug("Removing channels $unregistering")

        registering.forEach {
            val ch = guild.getTextChannelById(it) as? Channel ?: guild.getVoiceChannelById(
                    it) as? Channel ?: return@forEach
            GuildChannel.register(ch).get()
        }

        unregistering.forEach {
            GuildChannel.unregister(it).get()
        }

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

