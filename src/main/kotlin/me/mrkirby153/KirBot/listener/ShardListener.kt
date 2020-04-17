package me.mrkirby153.KirBot.listener

import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.executors.admin.CommandStats
import me.mrkirby153.KirBot.database.models.DiscordUser
import me.mrkirby153.KirBot.database.models.guild.DiscordGuild
import me.mrkirby153.KirBot.database.models.guild.Role
import me.mrkirby153.KirBot.event.EventPriority
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.AccessModule
import me.mrkirby153.KirBot.modules.AdminControl
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.server.UserPersistenceHandler
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.KirBot.utils.sanitize
import me.mrkirby153.KirBot.utils.settings.GuildSettings
import me.mrkirby153.kcutils.utils.IdGenerator
import net.dv8tion.jda.api.events.ShutdownEvent
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.text.update.GenericTextChannelUpdateEvent
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdatePermissionsEvent
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.voice.update.GenericVoiceChannelUpdateEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.guild.update.GuildUpdateIconEvent
import net.dv8tion.jda.api.events.guild.update.GuildUpdateNameEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.role.RoleCreateEvent
import net.dv8tion.jda.api.events.role.RoleDeleteEvent
import net.dv8tion.jda.api.events.role.update.GenericRoleUpdateEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ShardListener @Inject constructor(private val accessModule: AccessModule, private val userPersistenceHandler: UserPersistenceHandler) {

    private val idGenerator = IdGenerator(IdGenerator.ALPHA + IdGenerator.NUMBERS)
    private val guildLeavesIgnore = mutableSetOf<String>()

    @Subscribe
    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        userPersistenceHandler.restore(event.user, event.guild)
        userPersistenceHandler.deleteBackup(event.member)
        val user = Model.where(DiscordUser::class.java, "id", event.user.id).first()
        if (user == null) {
            val u = DiscordUser()
            u.id = event.user.id
            u.username = event.user.name
            u.discriminator = event.user.discriminator.toInt()
            u.create()
        }
    }

    @Subscribe
    fun onGuildMemberLeave(event: GuildMemberRemoveEvent) {
        val member = event.member
        if(member != null) {
            userPersistenceHandler.createBackup(member)
        } else {
            Bot.LOG.debug("cannot create a backup of ${event.user} because they are not cached")
        }
    }

    @Subscribe
    fun onGuildUpdateName(event: GuildUpdateNameEvent) {
        val g = event.guild.kirbotGuild.discordGuild
        g.name = event.newName
        g.save()
    }

    @Subscribe
    fun onGuildIconUpdate(event: GuildUpdateIconEvent) {
        val g = event.guild.kirbotGuild.discordGuild
        g.iconId = event.newIconId
        g.save()
    }

    @Subscribe
    fun onUserUpdateName(event: UserUpdateNameEvent) {
        val user = Model.where(DiscordUser::class.java, "id", event.user.id).first() ?: return
        user.username = event.user.name
        user.discriminator = event.user.discriminator.toInt()
        user.save()
    }

    @Subscribe
    fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        userPersistenceHandler.restoreVoiceState(event.member.user, event.guild)
        userPersistenceHandler.deleteBackup(event.member)
    }

    @Subscribe
    fun onMessage(event: GuildMessageReceivedEvent) {
        CommandStats.incMessage(event.guild)
    }

    @Subscribe
    fun onGuildLeave(event: GuildLeaveEvent) {
        val guild = event.guild
        if (!guildLeavesIgnore.contains(event.guild.id)) {
            AdminControl.log("Left guild ${guild.name} (`${guild.id}`)")
        }
        guildLeavesIgnore.remove(event.guild.id)
        Model.where(DiscordGuild::class.java, "id", event.guild.id).first()?.delete()
        event.guild.kirbotGuild.onPart()
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    fun onGuildJoin(event: GuildJoinEvent) {
        fun getGuildInfo(): String {
            val guild = event.guild
            var userCount = 0
            var botCount = 0
            guild.members.forEach {
                if (it.user.isBot)
                    botCount++
                else
                    userCount++
            }

            return buildString {
                appendln("**${guild.name.sanitize()}**")
                appendln("ID: `${guild.id}`")
                appendln("Owner: ${guild.owner?.user?.logName ?: "Unknown"}")
                appendln()
                appendln("Members: ${guild.members.size}")
                appendln(":bust_in_silhouette: $userCount")
                appendln(":robot: $botCount")
            }
        }


        if (accessModule.onList(event.guild, AccessModule.WhitelistMode.BLACKLIST)) {
            Bot.LOG.debug("left guild ${event.guild.id} because it was blacklisted")
            AdminControl.log("Attempted to join blacklisted guild\n${getGuildInfo()}")
            guildLeavesIgnore.add(event.guild.id)
            event.guild.leave().queue()
            return
        }
        if (Bot.properties.getOrDefault("guild-whitelist", "false").toString().toBoolean()) {
            if (!accessModule.onList(event.guild, AccessModule.WhitelistMode.WHITELIST)) {
                Bot.LOG.debug("Left guild ${event.guild.id} because it was not whitelisted")
                AdminControl.log("Attempted to join non-whitelisted guild\n${getGuildInfo()}")
                guildLeavesIgnore.add(event.guild.id)
                event.guild.leave().queue()
                return
            }
        }
        AdminControl.log("Joined guild \n${getGuildInfo()}")
        event.guild.kirbotGuild.loadSettings()
        Bot.scheduler.schedule({
            event.guild.kirbotGuild.sync()
        }, 0, TimeUnit.MILLISECONDS)
    }

    @Subscribe
    fun onTextChannelDelete(event: TextChannelDeleteEvent) {
        Model.where(me.mrkirby153.KirBot.database.models.Channel::class.java, "id",
                event.channel.id).delete()
    }

    @Subscribe
    fun onVoiceChannelDelete(event: VoiceChannelDeleteEvent) {
        Model.where(me.mrkirby153.KirBot.database.models.Channel::class.java, "id",
                event.channel.id).delete()
    }

    @Subscribe
    fun onTextChannelCreate(event: TextChannelCreateEvent) {
        val channel = me.mrkirby153.KirBot.database.models.Channel()
        channel.id = event.channel.id
        channel.guild = event.guild
        channel.name = event.channel.name
        channel.type = me.mrkirby153.KirBot.database.models.Channel.Type.TEXT
        channel.hidden = false
        channel.create()
    }

    @Subscribe
    fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
        val channel = me.mrkirby153.KirBot.database.models.Channel()
        channel.id = event.channel.id
        channel.guild = event.guild
        channel.name = event.channel.name
        channel.type = me.mrkirby153.KirBot.database.models.Channel.Type.VOICE
        channel.hidden = false
        channel.create()
    }

    @Subscribe
    fun onGenericTextChannelUpdate(event: GenericTextChannelUpdateEvent<*>) {
        Model.where(me.mrkirby153.KirBot.database.models.Channel::class.java, "id",
                event.channel.id).first()?.updateChannel()
    }

    @Subscribe
    fun onGenericVoiceChannelUpdate(event: GenericVoiceChannelUpdateEvent<*>) {
        Model.where(me.mrkirby153.KirBot.database.models.Channel::class.java, "id",
                event.channel.id).first()?.updateChannel()
    }

    @Subscribe
    fun onRoleCreate(event: RoleCreateEvent) {
        val role = Role()
        role.role = event.role
        role.guild = event.guild
        role.save()
    }

    @Subscribe
    fun onGenericRoleUpdate(event: GenericRoleUpdateEvent<*>) {
        val role = Model.where(Role::class.java, "id", event.role.id).first() ?: return
        role.updateRole()
        role.save()
    }

    @Subscribe
    fun onRoleDelete(event: RoleDeleteEvent) {
        // Delete the selfrole entry if the role is deleted
        if (event.role.id in event.guild.kirbotGuild.getSelfroles())
            event.guild.kirbotGuild.removeSelfrole(event.role.id)
        Model.where(Role::class.java, "id", event.role.id).delete()
        // If the role is the muted role, delete it
        val mutedRole = GuildSettings.mutedRole.nullableGet(event.guild)
        if (mutedRole != null && event.role.id == mutedRole) {
            GuildSettings.mutedRole.set(event.guild, null)
        }
    }

    @Subscribe
    fun shutdownEvent(event: ShutdownEvent) {
        Bot.LOG.debug("Shard ${event.jda.shardInfo.shardId} is closing, purging guilds from memory")
        event.jda.guilds.forEach { guild ->
            KirBotGuild.remove(guild)
        }
    }
}