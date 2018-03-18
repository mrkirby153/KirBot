package me.mrkirby153.KirBot.listener

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.DiscordUser
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.guild.GuildMember
import me.mrkirby153.KirBot.database.models.guild.GuildMemberRole
import me.mrkirby153.KirBot.database.models.guild.Role
import me.mrkirby153.KirBot.database.models.guild.ServerSettings
import me.mrkirby153.KirBot.modules.AdminControl
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.sharding.Shard
import me.mrkirby153.KirBot.utils.kirbotGuild
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.text.update.GenericTextChannelUpdateEvent
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
import net.dv8tion.jda.core.events.user.UserNameUpdateEvent
import net.dv8tion.jda.core.events.user.UserOnlineStatusUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit

class ShardListener(val shard: Shard, val bot: Bot) : ListenerAdapter() {

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val member = Model.first(GuildMember::class.java, Pair("server_id", event.guild.id),
                Pair("user_id", event.user.id))
        if (member == null) {
            Bot.LOG.debug("User has not joined before, or persistence is disabled")
            val m = GuildMember()
            m.id = Model.randomId()
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
        val user = Model.first(DiscordUser::class.java, event.user.id)
        if (user == null) {
            val u = DiscordUser()
            u.username = event.user.name
            u.discriminator = event.user.discriminator.toInt()
            u.create()
        }
    }

    override fun onUserNameUpdate(event: UserNameUpdateEvent) {
        val user = Model.first(DiscordUser::class.java, event.user.id) ?: return
        user.username = event.user.name
        user.discriminator = event.user.discriminator.toInt()
        user.save()
    }

    override fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        if (!event.guild.kirbotGuild.settings.persistence) { // Delete the user if persistence is disabled
            val member = Model.first(GuildMember::class.java, Pair("server_id", event.guild.id),
                    Pair("user_id", event.user.id))
            member?.roles?.forEach { it.delete() }
            member?.delete()
        }
    }

    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        event.roles.forEach {
            Bot.LOG.debug("Adding role ${it.name}(${it.id}) to ${event.user} ")
            val role = GuildMemberRole()
            role.id = Model.randomId()
            role.role = it
            role.user = event.user
            role.save()
        }
    }

    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        event.roles.forEach {
            Model.get(GuildMemberRole::class.java, Pair("server_id", event.guild.id),
                    Pair("user_id", event.user.id), Pair("role_id", it.id)).forEach { it.delete() }
        }
    }

    override fun onGuildMemberNickChange(event: GuildMemberNickChangeEvent) {
        Model.first(GuildMember::class.java, Pair("server_id", event.guild.id),
                Pair("user_id", event.user.id))?.run {
            nick = event.newNick
            save()
        }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        val guild = event.guild
        AdminControl.log("Left guild ${guild.name} (`${guild.id}`)")
        Model.first(ServerSettings::class.java, event.guild.id)?.delete()
        // TODO 1/20/18: Delete the relations as well
        event.guild.kirbotGuild.onPart()
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        AdminControl.log(
                "Joined guild ${event.guild.name} (`${event.guild.id}`) [${event.guild.members.size} members]")
        Bot.scheduler.schedule({
            event.guild.kirbotGuild.sync()
        }, 0, TimeUnit.MILLISECONDS)
    }

    override fun onUserOnlineStatusUpdate(event: UserOnlineStatusUpdateEvent) {
        Bot.seenStore.updateOnlineStatus(event.user.mutualGuilds[0].getMember(event.user))
    }

    override fun onTextChannelDelete(event: TextChannelDeleteEvent) {
        Model.first(me.mrkirby153.KirBot.database.models.Channel::class.java,
                event.channel.id)?.delete()
    }

    override fun onVoiceChannelDelete(event: VoiceChannelDeleteEvent) {
        Model.first(me.mrkirby153.KirBot.database.models.Channel::class.java,
                event.channel.id)?.delete()
    }

    override fun onTextChannelCreate(event: TextChannelCreateEvent) {
        val channel = me.mrkirby153.KirBot.database.models.Channel()
        channel.id = event.channel.id
        channel.guild = event.guild
        channel.name = event.channel.name
        channel.type = me.mrkirby153.KirBot.database.models.Channel.Type.TEXT
        channel.hidden = false
        channel.create()
    }

    override fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
        val channel = me.mrkirby153.KirBot.database.models.Channel()
        channel.id = event.channel.id
        channel.guild = event.guild
        channel.name = event.channel.name
        channel.type = me.mrkirby153.KirBot.database.models.Channel.Type.VOICE
        channel.hidden = false
        channel.create()
    }

    override fun onGenericTextChannelUpdate(event: GenericTextChannelUpdateEvent) {
        Model.first(me.mrkirby153.KirBot.database.models.Channel::class.java,
                event.channel.id)?.updateChannel()
    }

    override fun onGenericVoiceChannelUpdate(event: GenericVoiceChannelUpdateEvent) {
        Model.first(me.mrkirby153.KirBot.database.models.Channel::class.java,
                event.channel.id)?.updateChannel()
    }

    override fun onRoleCreate(event: RoleCreateEvent) {
        val role = Role()
        role.role = event.role
        role.guild = event.guild
        role.save()
    }

    override fun onGenericRoleUpdate(event: GenericRoleUpdateEvent) {
        val role = Model.first(Role::class.java, Pair("id", event.role.id)) ?: return
        role.updateRole()
        role.save()
    }

    override fun onRoleDelete(event: RoleDeleteEvent) {
        Model.first(Role::class.java, Pair("id", event.role.id))?.delete()
    }

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        val guild = event.guild.kirbotGuild

        if (!guild.musicManager.manualPause && guild.musicManager.audioPlayer.isPaused) {
            if (event.guild.selfMember.voiceState.inVoiceChannel() && event.guild.selfMember.voiceState.channel.id == event.channelJoined.id)
                Bot.LOG.debug("Resuming music in ${event.guild.id}:${event.channelJoined.id}")
            guild.musicManager.audioPlayer.isPaused = false
        }
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        val guild = event.guild.kirbotGuild
        if (inChannel(event.channelLeft, event.guild.selfMember))
            pauseIfEmpty(event.channelLeft, guild)
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        val guild = event.guild.kirbotGuild
        if (inChannel(event.channelJoined,
                        event.guild.selfMember) && !guild.musicManager.manualPause) {
            Bot.LOG.debug("Resuming in ${guild.id} as someone joined!")
            guild.musicManager.audioPlayer.isPaused = false
        } else {
            if (inChannel(event.channelLeft, event.guild.selfMember))
                pauseIfEmpty(event.channelLeft, guild)
        }
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