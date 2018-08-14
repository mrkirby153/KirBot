package me.mrkirby153.KirBot.listener

import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.DiscordUser
import me.mrkirby153.KirBot.database.models.guild.GuildMember
import me.mrkirby153.KirBot.database.models.guild.GuildMemberRole
import me.mrkirby153.KirBot.database.models.guild.Role
import me.mrkirby153.KirBot.database.models.guild.ServerSettings
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.AdminControl
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.sharding.Shard
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.kcutils.utils.IdGenerator
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
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
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
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
        val member = Model.where(GuildMember::class.java, "server_id", event.guild.id).where(
                "user_id", event.user.id).first()
        if (member == null) {
            Bot.LOG.debug("User has not joined before, or persistence is disabled")
            val m = GuildMember()
            m.id = idGenerator.generate(10)
            m.serverId = event.guild.id
            m.user = event.user
            m.save()
        } else {
            if (event.guild.kirbotGuild.settings.persistence) {
                Bot.LOG.debug("User has joined. Restoring their state")
                val controller = event.guild.controller
                controller.setNickname(event.member, member.nick).queue()
                controller.addRolesToMember(event.member,
                        member.roles.map { it.role }.filter { it != null }).queue()
            }
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

    override fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        event.guild.kirbotGuild.lock()
        if (!event.guild.kirbotGuild.settings.persistence) { // Delete the user if persistence is disabled
            val member = Model.where(GuildMember::class.java, "server_id", event.guild.id).where(
                    "user_id", event.user.id).first()
            member?.roles?.forEach { it.delete() }
            member?.delete()
        }
        event.guild.kirbotGuild.unlock()
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
        Model.where(ServerSettings::class.java, "id", event.guild.id).delete()
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
            event.guild.kirbotGuild.dispatchBackfill()
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
            Bot.LOG.debug("Our channel override was updated on ${event.channel.name} re-caching visibilities")
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
        if(settings.mutedRoleId == event.role.id) {
            settings.mutedRole = null
            settings.save()
        }
    }

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        val guild = event.guild.kirbotGuild
        guild.lock()
        if (!guild.musicManager.manualPause && guild.musicManager.audioPlayer.isPaused) {
            if (event.guild.selfMember.voiceState.inVoiceChannel() && event.guild.selfMember.voiceState.channel.id == event.channelJoined.id)
                Bot.LOG.debug("Resuming music in ${event.guild.id}:${event.channelJoined.id}")
            guild.musicManager.audioPlayer.isPaused = false
        }
        guild.unlock()
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        val guild = event.guild.kirbotGuild
        guild.lock()
        if (inChannel(event.channelLeft, event.guild.selfMember))
            pauseIfEmpty(event.channelLeft, guild)
        guild.unlock()
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        val guild = event.guild.kirbotGuild
        guild.lock()
        if (inChannel(event.channelJoined,
                        event.guild.selfMember) && !guild.musicManager.manualPause) {
            Bot.LOG.debug("Resuming in ${guild.id} as someone joined!")
            guild.musicManager.audioPlayer.isPaused = false
        } else {
            if (inChannel(event.channelLeft, event.guild.selfMember))
                pauseIfEmpty(event.channelLeft, guild)
        }
        guild.unlock()
    }


    private fun pauseIfEmpty(channel: Channel, guild: KirBotGuild) {
        if (channel.members.none { m -> m.user.id != channel.guild.selfMember.user.id }) {
            guild.musicManager.audioPlayer.isPaused = true
            Bot.LOG.debug(
                    "Pausing music in ${channel.guild.id}:${channel.id} as nobody is in the chanel")
        }
    }

    private fun inChannel(channel: Channel,
                          member: Member) = (member.voiceState.inVoiceChannel() && member.voiceState.channel.id == channel.id)
}