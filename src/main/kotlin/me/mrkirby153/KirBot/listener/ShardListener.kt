package me.mrkirby153.KirBot.listener

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.elements.Pair
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.DiscordUser
import me.mrkirby153.KirBot.database.models.guild.GuildMember
import me.mrkirby153.KirBot.database.models.guild.GuildMemberRole
import me.mrkirby153.KirBot.database.models.guild.Role
import me.mrkirby153.KirBot.database.models.guild.ServerSettings
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.AdminControl
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.server.UserPersistenceHandler
import me.mrkirby153.KirBot.sharding.Shard
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.kcutils.utils.IdGenerator
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
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceGuildDeafenEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceGuildMuteEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.role.RoleCreateEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.events.role.update.GenericRoleUpdateEvent
import net.dv8tion.jda.core.events.user.update.UserUpdateNameEvent
import net.dv8tion.jda.core.events.user.update.UserUpdateOnlineStatusEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit

class ShardListener(val shard: Shard, val bot: Bot) : ListenerAdapter() {

    private val idGenerator = IdGenerator(IdGenerator.ALPHA + IdGenerator.NUMBERS)

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        event.guild.kirbotGuild.lock()
        UserPersistenceHandler.restore(event.user, event.guild)
        val member = Model.where(GuildMember::class.java, "server_id", event.guild.id).where(
                "user_id", event.user.id).first()
        if (member == null) {
            GuildMember(event.member).save()
        } else {
            member.user = event.member.user
            member.nick = event.member.nickname
            member.save()
        }

        val user = Model.where(DiscordUser::class.java, "id", event.user.id).first()
        if (user == null) {
            val u = DiscordUser()
            u.id = event.user.id
            u.username = event.user.name
            u.discriminator = event.user.discriminator.toInt()
            u.create()
        }
        event.guild.kirbotGuild.unlock()
    }

    override fun onUserUpdateName(event: UserUpdateNameEvent) {
        val user = Model.where(DiscordUser::class.java, "id", event.user.id).first() ?: return
        user.username = event.user.name
        user.discriminator = event.user.discriminator.toInt()
        user.save()
    }

    override fun onGuildVoiceGuildDeafen(event: GuildVoiceGuildDeafenEvent) {
        Model.where(GuildMember::class.java, "server_id", event.guild.id).where("user_id",
                event.member.user.id).update(Pair("deafened", event.isGuildDeafened))
    }

    override fun onGuildVoiceGuildMute(event: GuildVoiceGuildMuteEvent) {
        Model.where(GuildMember::class.java, "server_id", event.guild.id).where("user_id",
                event.member.user.id).update(Pair("muted", event.isGuildMuted))
    }

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        UserPersistenceHandler.restoreVoiceState(event.member.user, event.guild)
    }

    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        event.roles.forEach {
            Bot.LOG.debug("Adding role ${it.name}(${it.id}) to ${event.user} ")
            val role = GuildMemberRole()
            role.id = this.idGenerator.generate(10)
            role.role = it
            role.user = event.user
            role.save()
        }
        if (event.member == event.guild.selfMember) {
            Bot.LOG.debug("We had a role added, re-caching visibilities")
            event.guild.kirbotGuild.cacheVisibilities()
        }
    }

    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        event.roles.forEach {
            Model.where(GuildMemberRole::class.java, "server_id", event.guild.id).where("user_id",
                    event.user.id).where("role_id", it.id).get().forEach { it.delete() }
        }
        if (event.member == event.guild.selfMember) {
            Bot.LOG.debug("We had a role removed, re-caching visibilities")
            event.guild.kirbotGuild.cacheVisibilities()
        }
    }

    override fun onGuildMemberNickChange(event: GuildMemberNickChangeEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        Model.where(GuildMember::class.java, "server_id", event.guild.id).where("user_id",
                event.user.id).first()?.run {
            nick = event.newNick
            save()
        }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        val guild = event.guild
        AdminControl.log("Left guild ${guild.name} (`${guild.id}`)")
        Model.where(ServerSettings::class.java, "id", event.guild.id).first().delete()
        event.guild.kirbotGuild.onPart()
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
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
        Bot.scheduler.schedule({
            event.guild.kirbotGuild.sync()
            event.guild.kirbotGuild.completeBackfill()
        }, 0, TimeUnit.MILLISECONDS)
    }

    override fun onUserUpdateOnlineStatus(event: UserUpdateOnlineStatusEvent) {
        Bot.seenStore.updateOnlineStatus(event.user.mutualGuilds[0].getMember(event.user))
    }

    override fun onTextChannelDelete(event: TextChannelDeleteEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        Model.where(me.mrkirby153.KirBot.database.models.Channel::class.java, "id",
                event.channel.id).delete()
    }

    override fun onVoiceChannelDelete(event: VoiceChannelDeleteEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        Model.where(me.mrkirby153.KirBot.database.models.Channel::class.java, "id",
                event.channel.id).delete()
    }

    override fun onTextChannelCreate(event: TextChannelCreateEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        val channel = me.mrkirby153.KirBot.database.models.Channel()
        channel.id = event.channel.id
        channel.guild = event.guild
        channel.name = event.channel.name
        channel.type = me.mrkirby153.KirBot.database.models.Channel.Type.TEXT
        channel.hidden = false
        channel.create()
    }

    override fun onTextChannelUpdatePermissions(event: TextChannelUpdatePermissionsEvent) {
        val selfRoles = event.guild.selfMember.roles
        val matchedRoles = event.changedRoles.filter { it in selfRoles }
        if (matchedRoles.isNotEmpty() || event.guild.selfMember in event.changedMembers) {
            Bot.LOG.debug(
                    "Our channel override was updated on ${event.channel.name} re-caching visibilities")
            event.guild.kirbotGuild.cacheVisibilities()
        }
    }

    override fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        val channel = me.mrkirby153.KirBot.database.models.Channel()
        channel.id = event.channel.id
        channel.guild = event.guild
        channel.name = event.channel.name
        channel.type = me.mrkirby153.KirBot.database.models.Channel.Type.VOICE
        channel.hidden = false
        channel.create()
    }

    override fun onGenericTextChannelUpdate(event: GenericTextChannelUpdateEvent<*>) {
        if (!event.guild.kirbotGuild.ready)
            return
        Model.where(me.mrkirby153.KirBot.database.models.Channel::class.java, "id",
                event.channel.id).first()?.updateChannel()
    }

    override fun onGenericVoiceChannelUpdate(event: GenericVoiceChannelUpdateEvent<*>) {
        if (!event.guild.kirbotGuild.ready)
            return
        Model.where(me.mrkirby153.KirBot.database.models.Channel::class.java, "id",
                event.channel.id).first()?.updateChannel()
    }

    override fun onRoleCreate(event: RoleCreateEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        val role = Role()
        role.role = event.role
        role.guild = event.guild
        role.save()
    }

    override fun onGenericRoleUpdate(event: GenericRoleUpdateEvent<*>) {
        if (!event.guild.kirbotGuild.ready)
            return
        val role = Model.where(Role::class.java, "id", event.role.id).first() ?: return
        role.updateRole()
        role.save()
    }

    override fun onRoleDelete(event: RoleDeleteEvent) {
        // Delete the selfrole entry if the role is deleted
        if (event.role.id in event.guild.kirbotGuild.getSelfroles())
            event.guild.kirbotGuild.removeSelfrole(event.role.id)
        Model.where(Role::class.java, "id", event.role.id).delete()
        // If the role is the muted role, delete it
        val settings = event.guild.kirbotGuild.settings
        if (settings.mutedRoleId == event.role.id) {
            settings.mutedRole = null
            settings.save()
        }
    }
}