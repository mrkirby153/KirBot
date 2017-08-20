package me.mrkirby153.KirBot.listener

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.utils.Time
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import java.util.concurrent.TimeUnit

class AntiSpamListener(val shard: Shard) : ListenerAdapter() {

    // 5 seconds, 30 seconds, 1 minute, 5 minutes, 30 minutes
    val MUTE_TIME_MILLIS = arrayOf(5000, 30000, 60000, 300000, 1800 * 1000)

    val MUTE_TIME_RESET = /*3600 * 1000*/ 45 * 1000 // 1 hour

    val LINES = 5
    val SECONDS = 3

    val lastMessage = mutableMapOf<String, Long>()

    val messageCounter = mutableMapOf<String, Int>()

    val muteLevel = mutableMapOf<String, Int>()
    val muteLevelReset = mutableMapOf<String, Long>()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val channel = event.textChannel as? TextChannel ?: return
        val lastMessage = this.lastMessage.getOrDefault(event.author.id, -1)
        if (muteLevelReset.containsKey(event.author.id)) {
            if (muteLevelReset.getOrDefault(event.author.id, 0) < System.currentTimeMillis()) {
                muteLevelReset.remove(event.author.id)
                muteLevel.remove(event.author.id)
                Bot.LOG.debug("Resetting mute time for ${event.author.name}")
            }
        }
        this.lastMessage[event.author.id] = System.currentTimeMillis()
        if (lastMessage == -1L) {
            return
        }
        if (lastMessage + SECONDS * 1000 > System.currentTimeMillis()) {
            val msgCounter = messageCounter.getOrDefault(event.author.id, 0) + 1
            Bot.LOG.debug("Last message by ${event.author.name} in less than $SECONDS seconds. Incrementing to $msgCounter")
            messageCounter[event.author.id] = msgCounter
        } else {
            messageCounter.remove(event.author.id)
        }
        if (messageCounter.getOrDefault(event.author.id, 0) >= LINES) {
            Bot.LOG.debug("${event.author.name} has sent more than $LINES messages in $SECONDS seconds, muting...")
            val override = channel.getPermissionOverride(event.member) ?: channel.createPermissionOverride(event.member).complete()
            val muteLevel = Math.min((muteLevel[event.author.id] ?: -1) + 1, MUTE_TIME_MILLIS.size)
            this.muteLevel[event.author.id] = muteLevel
            this.muteLevelReset[event.author.id] = System.currentTimeMillis() + MUTE_TIME_RESET
            val muteTime = MUTE_TIME_MILLIS[muteLevel]
            override.manager.deny(Permission.MESSAGE_WRITE).queue {
                channel.sendMessage("${event.author.asMention} You have been muted for " +
                        "${Time.format(1, muteTime.toLong(), Time.TimeUnit.FIT)}. Limit is $LINES messages in $SECONDS seconds").queue {
                    val ra = if (override.denied.size > 1) {
                        if (override.denied.contains(Permission.MESSAGE_WRITE))
                            override.manager.clear(Permission.MESSAGE_WRITE)
                        else
                            null
                    } else {
                        override.delete()
                    }
                    ra?.queueAfter(muteTime.toLong(), TimeUnit.MILLISECONDS) {
                        event.channel.sendMessage("${event.author.asMention} You have been unmuted").queue()
                    }
                }
            }
        }
    }

}