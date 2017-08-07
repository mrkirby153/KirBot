package me.mrkirby153.KirBot.listener

import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.server.LogField
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.awt.Color
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

class AntiSpamListener(val shard: Shard) : ListenerAdapter() {

    val MUTE_TIME = arrayOf(1, 5, 10, 30)

    val STRIKE_THRESHOLD = 5
    val STRIKE_RESET_TIMEOUT = 30000L // 30 seconds
    val STRIKE_TIMING = 1500L // 1.5 seconds

    val MUTE_TIME_RESET = 1000 * 3600 // 1 Hour

    val lastMessageSent = mutableMapOf<String, Long>()

    val muteLevel = mutableMapOf<String, Int>()
    val muteLevelExpires = mutableMapOf<String, Long>()
    val strikeLevel = mutableMapOf<String, Int>()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val channel = event.textChannel as? TextChannel ?: return
        if (event.author.isBot)
            return

        if (!shard.getServerData(event.guild).spamFilterEnabled(channel))
            return

        val lastMessage = lastMessageSent[event.author.id] ?: 0
        if (lastMessage != 0L) {
            if (lastMessage + STRIKE_RESET_TIMEOUT <= System.currentTimeMillis() && strikeLevel.containsKey(event.author.id)) {
                println("[*] Resetting strikes for ${event.author.name}")
                strikeLevel.remove(event.author.id)
            }

            if ((muteLevelExpires[event.author.id] ?: 0) <= System.currentTimeMillis()) {
                println("[*] Resetting mute level for ${event.author.name}")
                muteLevelExpires.remove(event.author.id)
                muteLevel.remove(event.author.id)
            }

            if (lastMessage + STRIKE_TIMING > System.currentTimeMillis()) {
                strikeLevel[event.author.id] = (strikeLevel[event.author.id] ?: 0) + 1
                println("[!] Issuing strike to ${event.author.name} (${strikeLevel[event.author.id]})")
            }

            val strikes = strikeLevel[event.author.id] ?: 0
            if (strikes == Math.floor(STRIKE_THRESHOLD * 0.75).toInt()) {
                event.channel.sendMessage(event.author.asMention + " Slow down your send rate or you will be temporarily muted!").queue()
            }
            if (strikes > STRIKE_THRESHOLD) {
                println("[!] Muting ${event.author.name}")
                val muteLevel = Math.min((this.muteLevel[event.author.id] ?: -1) + 1, MUTE_TIME.size)
                shard.getServerData(event.guild).logger.log("Spam Filter", "${event.author.name} has hit the spam filter",
                        Color.CYAN, LogField("Mute Time", "${MUTE_TIME[muteLevel]} minutes", false))
                // Mute the user
                val override = channel.getPermissionOverride(event.member) ?: channel.createPermissionOverride(event.member).complete()
                override.manager.deny(Permission.MESSAGE_WRITE).queue {
                    event.channel.sendMessage(event.author.asMention + " You have been muted for ${MUTE_TIME[muteLevel]} minutes because of spam").queue()

                    // Purge messages in the last 2 minutes or the last 100 messages
                    event.channel.history.retrievePast(100).queue { history ->
                        val messagesToDelete = mutableListOf<Message>()
                        history.forEach {
                            if (it.creationTime.isAfter(OffsetDateTime.now().minusMinutes(2)) && it.author == event.author) {
                                messagesToDelete.add(it)
                            }
                        }
                        channel.deleteMessages(messagesToDelete).queue()
                    }

                    val ra =
                            if (override.denied.size > 1) {
                                if (override.denied.contains(Permission.MESSAGE_WRITE))
                                    override.manager.clear(Permission.MESSAGE_WRITE)
                                else
                                    null
                            } else {
                                override.delete()
                            }
                    this.strikeLevel.remove(event.author.id)
                    ra?.queueAfter(MUTE_TIME[muteLevel].toLong(), TimeUnit.MINUTES) {
                        event.channel.sendMessage(event.author.asMention + " You have been unmuted!").queue()
                    }
                    this.muteLevelExpires[event.author.id] = System.currentTimeMillis() + MUTE_TIME_RESET
                    this.muteLevel[event.author.id] = muteLevel
                }
                strikeLevel.remove(event.author.id)
            }
        }
        lastMessageSent[event.author.id] = System.currentTimeMillis()
    }

}