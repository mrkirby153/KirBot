package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.b
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.localizeTime
import me.mrkirby153.KirBot.utils.mdEscape
import java.awt.Color
import java.util.concurrent.TimeUnit

@Command(name = "skip,next", arguments = ["[option:string]"])
class CommandSkip : BaseCommand() {

    val skipCooldown = mutableMapOf<String, Long>()

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!context.kirbotGuild.musicManager.settings.enabled) {
            return
        } else {
            Bot.LOG.debug("Music is disabled in ${context.guild.id}, ignoring")
        }
        val musicManager = context.kirbotGuild.musicManager
        val musicSettings = musicManager.settings

        if (!musicManager.playing) {
            throw CommandException("I am not playing anything right now")
        }
        var shouldHalt = false
        cmdContext.ifPresent<String>("option") { action ->
            if (action.equals("force", true) && context.author.getClearance(
                    context.guild).value >= Clearance.BOT_MANAGER.value) {
                forceSkip(context)
                shouldHalt = true
            }
        }
        if (shouldHalt)
            return
        val skipIn = skipCooldown[context.author.id] ?: 0
        if (System.currentTimeMillis() < skipIn || skipIn == -1L) {
            throw CommandException(buildString {
                append("You are doing that too much\n\n")
                if (skipIn == -1L)
                    append("Try again after your current poll expires")
                else
                    append("Try again in ${localizeTime(
                            ((skipIn - System.currentTimeMillis()) / 1000).toInt())}")
            })
        }

        val currentlyPlaying = musicManager.nowPlaying ?: throw CommandException("Nothing playing!")
        val skipTimer = musicSettings.skipTimer
        context.send().embed("Music") {
            color = Color.GREEN
            description {
                +b(context.author.name)
                +" has voted to skip the current track: \n"
                +currentlyPlaying.info.title.mdEscape() link currentlyPlaying.info.uri
                +"\nReact with :thumbsup: or :thumbsdown: to vote"
                +"\nWhichever has the most votes in ${localizeTime(skipTimer)} wins!"
            }
            timestamp {
                now()
            }
        }.rest().queue { m ->
            skipCooldown[context.author.id] = System.currentTimeMillis() + (skipTimer * 1000) + 1500
            if (musicSettings.skipCooldown > 0)
                skipCooldown[context.author.id] = (skipCooldown[context.author.id] ?: 0) + (musicSettings.skipCooldown * 1000).toLong()
            m.addReaction("\uD83D\uDC4D").queue() // Thumbs up
            m.addReaction("\uD83D\uDC4E").queue() // Thumbs down
            m.editMessage(embed("Music") {
                description { +"Voting has ended. Check newer messages for results" }
                color = Color.RED
            }.build()).queueAfter(skipTimer.toLong(), TimeUnit.SECONDS) {
                if (musicManager.nowPlaying != currentlyPlaying) {
                    context.send().embed("Music") {
                        color = Color.CYAN
                        description { +"The song has changed, canceling vote" }
                        timestamp { now() }
                    }.rest().queue()
                    return@queueAfter
                }

                var skip = 0
                var stay = 0
                it.reactions.forEach {
                    when (it.reactionEmote.name) {
                        "\uD83D\uDC4D" -> skip = it.count - 1
                        "\uD83D\uDC4E" -> stay = it.count - 1
                    }
                }
                context.send().embed("Music") {
                    color = Color.CYAN
                    description {
                        if (skip > stay) {
                            appendln("The vote has passed. Playing the next song")
                            context.kirbotGuild.musicManager.trackScheduler.playNextTrack()
                        } else {
                            appendln("The vote has failed.")
                        }
                    }
                    timestamp { now() }
                }.rest().queue()
            }
        }
    }


    private fun forceSkip(context: Context) {
        context.channel.sendMessage("Skipping song and playing the next...").queue()
        context.kirbotGuild.musicManager.trackScheduler.playNextTrack()
    }
}