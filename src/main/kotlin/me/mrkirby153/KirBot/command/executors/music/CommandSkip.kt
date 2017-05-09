package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.localizeTime
import net.dv8tion.jda.core.b
import net.dv8tion.jda.core.entities.Message
import java.awt.Color
import java.util.concurrent.TimeUnit

@Command(name = "skip", description = "Votes to skip the current song", category = "Music")
class CommandSkip : MusicCommand() {

    val skipCooldown = mutableMapOf<Long, Long>()

    override fun exec(message: Message, args: Array<String>) {
        if (!serverData.musicManager.trackScheduler.playing) {
            message.send().error("I'm not playing anything right now").queue()
            return
        }
        if (serverData.musicManager.adminOnly) {
            forceSkip(message)
            return
        }
        if (!args.isEmpty()) {
            if (args[0].toLowerCase() == "force") {
                val clearance = message.author.getClearance(guild)
                if (clearance.value >= Clearance.SERVER_ADMINISTRATOR.value) {
                    forceSkip(message)
                    return
                }
            }
        }
        val skipIn = skipCooldown[message.author.idLong] ?: 0
        if (System.currentTimeMillis() < skipIn || skipIn == -1L) {
            val phrase = if (skipIn == -1L) "Try again after your current poll expires!" else "Try again in " +
                    localizeTime(((skipIn - System.currentTimeMillis()) / 1000).toInt())
            message.send().error("You are doing that too much! **$phrase**").queue()
            return
        }
        // Start a vote
        val currentlyPlaying = serverData.musicManager.trackScheduler.nowPlaying
        val musicData = serverData.getMusicData()
        message.send().embed("Music") {
            color = Color.CYAN
            description = buildString {
                appendln(b(message.author.name))
                append(" has voted to **SKIP** the current track! ")
                appendln("React with :thumbsup: or :thumbsdown: to vote")
                appendln("Whichever has the most votes in ${localizeTime(musicData.skipTimer)} wins")
            }
        }.rest().queue { m ->
            skipCooldown[message.author.idLong] = -1
            m.addReaction("👍").queue()
            m.addReaction("👎").queue()

            m.editMessage(m.embeds[0].edit().apply {
                description = "Voting has ended! Check newer messages for results"
                clearFields()
            }.build()).queueAfter(musicData.skipTimer.toLong(), TimeUnit.SECONDS) {

                if (serverData.musicManager.trackScheduler.nowPlaying?.identifier != currentlyPlaying?.identifier) {
                    it.send().embed("Music") {
                        color = Color.CYAN
                        description = "Song has changed, canceling vote"
                    }.rest().queue()
                    return@queueAfter
                }

                var skip = 0
                var stay = 0

                it.reactions.forEach {
                    if (it.emote.name == "👎") stay = it.count - 1
                    if (it.emote.name == "👍") skip = it.count - 1
                }

                it.send().embed("Music") {
                    color = Color.CYAN
                    description = buildString {
                        if (musicData.skipCooldown > 0)
                            skipCooldown[message.author.idLong] = System.currentTimeMillis() + (musicData.skipCooldown * 1000)
                        else
                            skipCooldown.remove(message.author.idLong)
                        if (skip > stay) {
                            appendln("The vote has passed! Playing next song.")
                            serverData.musicManager.trackScheduler.playNext()
                        } else {
                            appendln("The vote has failed! The song will stay.")
                        }
                    }
                }.rest().queue()
            }
        }
    }

    private fun forceSkip(message: Message) {
        message.send().embed("Music") {
            color = Color.CYAN
            description = "Playing next song..."
        }.rest().queue()
        serverData.musicManager.trackScheduler.playNext()
    }
}