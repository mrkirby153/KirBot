package me.mrkirby153.KirBot.listener

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.command.CommandManager
import me.mrkirby153.KirBot.database.api.PanelAPI
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
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.awt.Color

class ShardListener(val shard: Shard, val bot: Bot) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent?) {
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

    override fun onTextChannelDelete(event: TextChannelDeleteEvent) {
        PanelAPI.unregisterChannel(event.channel.id).queue()
    }

    override fun onVoiceChannelDelete(event: VoiceChannelDeleteEvent) {
        PanelAPI.unregisterChannel(event.channel.id).queue()
    }

    override fun onTextChannelCreate(event: TextChannelCreateEvent) {
        PanelAPI.registerChannel(event.channel).queue()
    }

    override fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
        PanelAPI.registerChannel(event.channel).queue()
    }

    override fun onTextChannelUpdateName(event: TextChannelUpdateNameEvent) {
        PanelAPI.updateChannel(event.channel).queue()
    }

    override fun onVoiceChannelUpdateName(event: VoiceChannelUpdateNameEvent) {
        PanelAPI.updateChannel(event.channel).queue()
    }

    override fun onRoleCreate(event: RoleCreateEvent) {
        PanelAPI.createRole(event.role).queue()
    }

    override fun onGenericRoleUpdate(event: GenericRoleUpdateEvent) {
        PanelAPI.updateRole(event.role).queue()
    }

    override fun onRoleDelete(event: RoleDeleteEvent) {
        PanelAPI.deleteRole(event.role.id).queue()
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.reaction.emote.name == "\uD83D\uDDE8" /* Left speech bubble */) {
            event.channel.getMessageById(event.messageId).queue { message ->
                if (message != null) {
                    PanelAPI.getQuotesForGuild(event.guild).queue({ quotes ->
                        if (event.messageId !in quotes.map { it.messageId }) {
                            if (!message.content.isNullOrBlank())
                                PanelAPI.quote(message).queue {
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