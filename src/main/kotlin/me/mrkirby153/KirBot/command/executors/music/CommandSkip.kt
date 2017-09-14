package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.music.MusicManager
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.b
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.localizeTime
import java.awt.Color
import java.util.concurrent.TimeUnit

class CommandSkip: CmdExecutor() {

    val skipCooldown = mutableMapOf<String, Long>()

    override fun execute(context: Context, cmdContext: CommandContext) {
        val musicManager = context.data.musicManager
        val musicSettings = MusicManager.musicSettings[context.guild.id] ?: throw CommandException("Could not load music settings")

        if(!musicManager.playing){
            throw CommandException("I am not playing anything right now")
        }
        var shouldHalt = false
        cmdContext.has<String>("action") { action ->
            if(action.equals("force", true) && context.author.getClearance(context.guild).value >= Clearance.BOT_MANAGER.value){
                forceSkip(context)
                shouldHalt = true
            }
        }
        if(shouldHalt)
            return
        val skipIn = skipCooldown[context.author.id] ?: 0
        if(System.currentTimeMillis() < skipIn || skipIn == -1L){
            throw CommandException(buildString {
                append("You are doing that too much\n\n")
                if(skipIn == -1L)
                    append("Try again after your current poll expires")
                else
                    append("Try again in ${localizeTime(((skipIn - System.currentTimeMillis()) / 1000).toInt())}")
            })
        }

        val currentlyPlaying = musicManager.nowPlaying ?: throw CommandException("Nothing playing!")
        val skipTimer= musicSettings?.skipTimer ?: 300
        context.send().embed("Music") {
            setColor(Color.GREEN)
            setDescription(buildString {
                append(b(context.author.name))
                append(" has voted to skip the current track: \n")
                append(currentlyPlaying.info.title link currentlyPlaying.info.uri)
                append("\n React with :thumbsup: or :thumbsdown: to vote")
                append("\n Whichever has the most votes in ${localizeTime(skipTimer)} wins!")
            })
        }.rest().queue{ m ->
            skipCooldown[context.author.id] = -1
            m.addReaction("\uD83D\uDC4D").queue() // Thumbs up
            m.addReaction("\uD83D\uDC4E").queue() // Thumbs down
            m.editMessage(embed("Music"){
                setDescription("Voting has ended. Check newer messages for results")
                setColor(Color.RED)
            }.build()).queueAfter(skipTimer.toLong(),TimeUnit.SECONDS){
                if(musicManager.nowPlaying != currentlyPlaying){
                    context.send().embed("Music"){
                        setColor(Color.CYAN)
                        setDescription("The song has changed, canceling vote")
                    }.rest().queue()
                    return@queueAfter
                }

                var skip = 0
                var stay = 0
                it.reactions.forEach {
                    when(it.emote.name) {
                        "\uD83D\uDC4D" -> skip = it.count - 1
                        "\uD83D\uDC4E" -> stay = it.count - 1
                    }
                }
                context.send().embed("Music") {
                    setColor(Color.CYAN)
                    setDescription(buildString {
                        if (musicSettings.skipCooldown > 0)
                            skipCooldown[context.author.id] = System.currentTimeMillis() + (musicSettings.skipCooldown * 1000)
                        else
                            skipCooldown.remove(context.author.id)
                        if (skip > stay) {
                            appendln("The vote has passed. Playing the next song")
                            context.data.musicManager.trackScheduler.playNextTrack()
                        } else {
                            appendln("The vote has failed.")
                        }
                    })
                }.rest().queue()
            }
        }
    }


    private fun forceSkip(context: Context){
        context.channel.sendMessage("Skipping song and playing the next...").queue()
        context.data.musicManager.trackScheduler.playNextTrack()
    }
}