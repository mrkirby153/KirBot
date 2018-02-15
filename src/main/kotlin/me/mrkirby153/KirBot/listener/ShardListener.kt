package me.mrkirby153.KirBot.listener

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandExecutor
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.Quote
import me.mrkirby153.KirBot.database.models.guild.GuildMember
import me.mrkirby153.KirBot.database.models.guild.GuildMemberRole
import me.mrkirby153.KirBot.database.models.guild.Role
import me.mrkirby153.KirBot.database.models.guild.ServerSettings
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.sharding.Shard
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.RED_CROSS
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.kirbotGuild
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdateNameEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.update.VoiceChannelUpdateNameEvent
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
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.MessageUpdateEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.events.role.RoleCreateEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.events.role.update.GenericRoleUpdateEvent
import net.dv8tion.jda.core.events.user.UserOnlineStatusUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.awt.Color
import java.util.concurrent.TimeUnit

class ShardListener(val shard: Shard, val bot: Bot) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.author.isFake)
            Bot.seenStore.update(event.author, event.guild)

        if (event.author == shard.selfUser)
            return

        val context = Context(event)

        CommandExecutor.execute(context)
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        // If the message was edited
       if(event.channel.latestMessageId == event.messageId){
           // Only process if it's the last message
           if(event.author == shard.selfUser){
               return // Lets not update the message
           }
           val context = Context(shard, event.message)
           CommandExecutor.execute(context)
       }
    }

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
        // TODO 1/20/18: Broadcast a log event
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        Model.first(ServerSettings::class.java, event.guild.id)?.delete()
        // TODO 1/20/18: Delete the relations as well
        event.guild.kirbotGuild.onPart()
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
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

    override fun onTextChannelUpdateName(event: TextChannelUpdateNameEvent) {
        val chan = Model.first(me.mrkirby153.KirBot.database.models.Channel::class.java,
                event.channel.id) ?: return
        chan.name = event.channel.name
        chan.save()
    }

    override fun onVoiceChannelUpdateName(event: VoiceChannelUpdateNameEvent) {
        val chan = Model.first(me.mrkirby153.KirBot.database.models.Channel::class.java,
                event.channel.id) ?: return
        chan.name = event.channel.name
        chan.save()
    }

    override fun onRoleCreate(event: RoleCreateEvent) {
        val role = Role()
        role.role = event.role
        role.guild = event.guild
        role.save()
    }

    override fun onGenericRoleUpdate(event: GenericRoleUpdateEvent) {
        val role = Model.first(Role::class.java, Pair("id", event.role.id)) ?: return
        role.role = event.role
        role.save()
    }

    override fun onRoleDelete(event: RoleDeleteEvent) {
        Model.first(Role::class.java, Pair("id", event.role.id))?.delete()
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        // TODO 2017-09-07: Quoting should be a privilege, not a right
        if (event.reaction.reactionEmote.name == "\uD83D\uDDE8" /* Left speech bubble */) {
            event.channel.getMessageById(event.messageId).queue { message ->
                if (message != null) {
                    val q = Model.first(Quote::class.java, Pair("message_id", event.messageId))
                    if (q != null)
                        return@queue // The quote already exists
                    val quote = Quote()
                    quote.serverId = event.guild.id
                    quote.messageId = event.messageId
                    quote.user = message.author.name
                    quote.content = message.contentDisplay
                    quote.save()
                    event.channel.sendMessage(embed("Quote") {
                        color = Color.BLUE
                        description {
                            +"A new quote has been made by  `${event.member.user.name}#${event.member.user.discriminator}`"
                        }
                        fields {
                            field {
                                title = "ID"
                                inline = true
                                description = quote.id.toString()
                            }
                            field {
                                title = "Message"
                                inline = false
                                description = quote.content
                            }
                        }
                    }.build()).queue()
                }
            }
        }
        if (event.reaction.reactionEmote.name == RED_CROSS) {
            event.channel.getMessageById(event.messageId).queue { msg ->
                if (msg.author.id == event.guild.selfMember.user.id) {
                    if (msg.contentRaw.startsWith("\u2063")) {
                        if (msg.mentionedUsers.contains(event.user)) {
                            msg.delete().queue()
                        }
                    }
                }
            }
        }
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