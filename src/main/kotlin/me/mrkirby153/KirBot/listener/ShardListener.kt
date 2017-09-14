package me.mrkirby153.KirBot.listener

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.command.CommandManager
import me.mrkirby153.KirBot.database.api.GuildChannel
import me.mrkirby153.KirBot.database.api.GuildRole
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.database.api.Quote
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.sync
import net.dv8tion.jda.core.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.core.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.text.update.TextChannelUpdateNameEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.core.events.channel.voice.update.VoiceChannelUpdateNameEvent
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.events.role.RoleCreateEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.events.role.update.GenericRoleUpdateEvent
import net.dv8tion.jda.core.events.user.UserOnlineStatusUpdateEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.awt.Color

class ShardListener(val shard: Shard, val bot: Bot) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if(event != null){
            Bot.seenStore.update(event.author, event.guild)
        }

        if (event!!.author == shard.selfUser)
            return

        val context = Context(event)

        CommandManager.execute(context, shard, event.guild)
    }

    override fun onGuildLeave(event: GuildLeaveEvent) {
        PanelAPI.unregisterServer(event.guild).queue()
    }

    override fun onGuildJoin(event: GuildJoinEvent) {
        PanelAPI.registerServer(event.guild).queue {
            event.guild.sync()
        }
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

    override fun onTextChannelUpdateName(event: TextChannelUpdateNameEvent){
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
                                        setColor(Color.BLUE)
                                        setDescription("A new quote has been made by `${event.member.user.name}#${event.member.user.discriminator}`")
                                        field("ID", true, it.id)
                                        field("Message", false, it.content)
                                    }.build()).queue()
                                }
                        }
                    })
                }
            }
        }
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent?) {
        val guild = event!!.guild
        // Ignore if KirBot isn't in that context
        if (guild.audioManager.connectedChannel != event.channelLeft) {
            return
        }

        val usersInChannel = guild.audioManager.connectedChannel.members.filter { it.user.id != guild.selfMember.user.id }.size

        if (usersInChannel < 1) {
            shard.getServerData(guild).musicManager.trackScheduler.reset()
        }

    }
}