package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.getClearance
import net.dv8tion.jda.core.b
import net.dv8tion.jda.core.entities.Message
import java.awt.Color
import java.util.concurrent.TimeUnit

@Command(name = "skip", description = "Votes to skip the current song")
class CommandSkip : MusicCommand() {
    override fun exec(message: Message, args: Array<String>) {
        if (!server.musicManager.trackScheduler.playing) {
            message.send().error("I'm not playing anything right now").queue()
            return
        }
        if(server.musicManager.adminOnly){
            forceSkip(message)
            return
        }
        if (!args.isEmpty()) {
            if (args[0].toLowerCase() == "force") {
                val clearance = message.author.getClearance(server)
                if (clearance.value >= Clearance.SERVER_ADMINISTRATOR.value) {
                    forceSkip(message)
                    return
                }
            }
        }
        // Start a vote
        val currentlyPlaying = server.musicManager.trackScheduler.nowPlaying
        message.send().embed("Music") {
            color = Color.CYAN
            description = buildString {
                appendln(b(message.author.name))
                append(" has voted to **SKIP** the current track! ")
                appendln("React with :thumbsup: or :thumbsdown: to vote")
                appendln("Whichever has the most votes in 30 secnds wins")
            }
        }.rest().queue { m ->
            m.addReaction("👍").queue()
            m.addReaction("👎").queue()

            m.editMessage(m.embeds[0].edit().apply {
                description = "Voting has ended! Check newer messages for results"
                clearFields()
            }.build()).queueAfter(30, TimeUnit.SECONDS) {

                if(server.musicManager.trackScheduler.nowPlaying?.identifier != currentlyPlaying?.identifier){
                    it.send().embed("Music"){
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
                        if (skip > stay) {
                            appendln("The vote has passed! Playing next song.")
                            server.musicManager.trackScheduler.playNext()
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
        server.musicManager.trackScheduler.playNext()
    }
}