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
import me.mrkirby153.KirBot.modules.AdminControl
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.server.UserPersistenceHandler
import me.mrkirby153.KirBot.utils.SettingsRepository
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.kcutils.utils.IdGenerator
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.text.update.GenericTextChannelUpdateEvent
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdatePermissionsEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.update.GenericVoiceChannelUpdateEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.core.events.guild.update.GuildUpdateIconEvent
import net.dv8tion.jda.core.events.guild.update.GuildUpdateNameEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.role.RoleCreateEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.events.role.update.GenericRoleUpdateEvent
import net.dv8tion.jda.core.events.user.update.UserUpdateNameEvent
import java.util.concurrent.TimeUnit

class ShardListener {

    private val idGenerator = IdGenerator(IdGenerator.ALPHA + IdGenerator.NUMBERS)

    @Subscribe
    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        UserPersistenceHandler.restore(event.user, event.guild)
        UserPersistenceHandler.deleteBackup(event.member)
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
    fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        UserPersistenceHandler.createBackup(event.member)
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
        UserPersistenceHandler.restoreVoiceState(event.member.user, event.guild)
        UserPersistenceHandler.deleteBackup(event.member)
    }

    @Subscribe
    fun onMessage(event: GuildMessageReceivedEvent) {
        CommandStats.incMessage(event.guild)
    }

    @Subscribe
    fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        if (event.member == event.guild.selfMember) {
            Bot.LOG.debug("We had a role added, re-caching visibilities")
            event.guild.kirbotGuild.cacheVisibilities()
        }
    }

    @Subscribe
    fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        if (event.member == event.guild.selfMember) {
            Bot.LOG.debug("We had a role removed, re-caching visibilities")
            event.guild.kirbotGuild.cacheVisibilities()
        }
    }

    @Subscribe
    fun onGuildLeave(event: GuildLeaveEvent) {
        val guild = event.guild
        AdminControl.log("Left guild ${guild.name} (`${guild.id}`)")
        Model.where(DiscordGuild::class.java, "id", event.guild.id).first().delete()
        event.guild.kirbotGuild.onPart()
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    fun onGuildJoin(event: GuildJoinEvent) {
        // Check the guild whitelist
        if (Bot.properties.getOrDefault("guild-whitelist", "false").toString().toBoolean()) {
            ModuleManager[Redis::class.java].getConnection().use { con ->
                val status = (con.get("whitelist:${event.guild.id}") ?: "false").toBoolean()
                if (!status) {
                    Bot.LOG.debug(
                            "Left guild ${event.guild.id} because it was not on the whitelist!")
                    event.guild.leave().queue()
                    return
                } else {
                    con.del("whitelist:${event.guild.id}")
                    Bot.LOG.debug("Joined whitelisted guild ${event.guild.id}")
                }
            }
        }
        AdminControl.log(
                "Joined guild ${event.guild.name} (`${event.guild.id}`) [${event.guild.members.size} members]")
        event.guild.kirbotGuild.loadSettings()
        Bot.scheduler.schedule({
            event.guild.kirbotGuild.sync()
            event.guild.kirbotGuild.dispatchBackfill()
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
    fun onTextChannelUpdatePermissions(event: TextChannelUpdatePermissionsEvent) {
        val selfRoles = event.guild.selfMember.roles
        val matchedRoles = event.changedRoles.filter { it in selfRoles }
        if (matchedRoles.isNotEmpty() || event.guild.selfMember in event.changedMembers) {
            Bot.LOG.debug(
                    "Our channel override was updated on ${event.channel.name} re-caching visibilities")
            event.guild.kirbotGuild.cacheVisibilities()
        }
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
        val mutedRole = SettingsRepository.get(event.guild, "muted_role")
        if (mutedRole != null && event.role.id == mutedRole) {
            SettingsRepository.set(event.guild, "muted_role", null)
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