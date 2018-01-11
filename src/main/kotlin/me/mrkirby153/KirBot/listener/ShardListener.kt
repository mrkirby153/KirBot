package me.mrkirby153.KirBot.listener

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandExecutor
import me.mrkirby153.KirBot.database.api.GuildChannel
import me.mrkirby153.KirBot.database.api.GuildMember
import me.mrkirby153.KirBot.database.api.GuildRole
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.database.api.Quote
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

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        PanelAPI.getMembers(event.guild).queue { members ->
            if (event.member.user.id in members.map { it.userId } && event.guild.kirbotGuild.settings.persistence) {
                Bot.LOG.debug("User has previously joined, restoring state")
                GuildMember.get(event.member).queue { member ->
                    val guildController = event.guild.controller
                    guildController.setNickname(event.member, member.nick).queue()
                    guildController.addRolesToMember(event.member,
                            member.roles.map {
                                event.guild.getRoleById(it)
                            }.filter { it != null }).queue()
                }
            } else {
                Bot.LOG.debug("User has not joined before or persistence is disabled")
                GuildMember.create(event.member).queue()
            }
        }
    }

    override fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        if (!event.guild.kirbotGuild.settings.persistence) { // Delete the user if persistence is disabled
            GuildMember.get(event.member).queue {
                it.delete().queue()
            }
        }
    }

    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        if (event.guild.kirbotGuild.settings.persistence)
            GuildMember.get(event.member).queue { m ->
                event.roles.forEach {
                    m.addRole(it.id).queue()
                }
            }
    }

    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        if (event.guild.kirbotGuild.settings.persistence)
            GuildMember.get(event.member).queue { m ->
                event.roles.forEach {
                    m.removeRole(it.id).queue()
                }
            }
    }

    override fun onGuildMemberNickChange(event: GuildMemberNickChangeEvent) {
        GuildMember.get(event.member).queue { m ->
            if (m.needsUpdate())
                m.update().queue()
        }
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        PanelAPI.unregisterServer(event.guild).queue()
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
        GuildChannel.unregister(event.channel.id).queue()
    }

    override fun onVoiceChannelDelete(event: VoiceChannelDeleteEvent) {
        GuildChannel.unregister(event.channel.id).queue()
    }

    override fun onTextChannelCreate(event: TextChannelCreateEvent) {
        GuildChannel.register(event.channel).queue()
    }

    override fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
        GuildChannel.register(event.channel).queue()
    }

    override fun onTextChannelUpdateName(event: TextChannelUpdateNameEvent) {
        PanelAPI.getChannels(event.guild).queue {
            it.text.filter { it.channel.id == event.channel.id }.forEach { it.update().queue() }
        }
    }

    override fun onVoiceChannelUpdateName(event: VoiceChannelUpdateNameEvent) {
        PanelAPI.getChannels(event.guild).queue {
            it.voice.filter { it.channel.id == event.channel.id }.forEach { it.update().queue() }
        }
    }

    override fun onRoleCreate(event: RoleCreateEvent) {
        GuildRole.create(event.role).queue()
    }

    override fun onGenericRoleUpdate(event: GenericRoleUpdateEvent) {
        GuildRole.get(event.role).queue {
            it.update().queue()
        }
    }

    override fun onRoleDelete(event: RoleDeleteEvent) {
        GuildRole.get(event.role).queue {
            it.delete().queue()
        }
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        // TODO 2017-09-07: Quoting should be a privilege, not a right
        if (event.reaction.emote.name == "\uD83D\uDDE8" /* Left speech bubble */) {
            event.channel.getMessageById(event.messageId).queue { message ->
                if (message != null) {
                    Quote.get(event.guild).queue({ quotes ->
                        if (event.messageId !in quotes.map { it.messageId }) {
                            if (!message.content.isNullOrBlank())
                                Quote.create(message).queue {
                                    message.pin().queue()
                                    event.channel.sendMessage(embed("Quote") {
                                        color = Color.BLUE
                                        description {
                                            +"A new quote has been made by `${event.member.user.name}#${event.member.user.discriminator}`"
                                        }
                                        fields {
                                            field {
                                                title = "ID"
                                                inline = true
                                                description = it.id.toString()
                                            }
                                            field {
                                                title = "Message"
                                                inline = false
                                                description = it.content
                                            }
                                        }
                                    }.build()).queue()
                                }
                        }
                    })
                }
            }
        }
        if (event.reaction.emote.name == RED_CROSS) {
            event.channel.getMessageById(event.messageId).queue { msg ->
                if (msg.author.id == event.guild.selfMember.user.id) {
                    if (msg.rawContent.startsWith("\u2063")) {
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
        if (inChannel(event.channelJoined, event.guild.selfMember) && !guild.musicManager.manualPause) {
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