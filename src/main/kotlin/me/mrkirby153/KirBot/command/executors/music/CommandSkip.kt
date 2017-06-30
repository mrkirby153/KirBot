package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.b
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.localizeTime
import java.awt.Color
import java.util.concurrent.TimeUnit

class CommandSkip : MusicCommand() {

    val skipCooldown = mutableMapOf<Long, Long>()

    override fun exec(context: Context, cmdContext: CommandContext) {
        if (!context.data.musicManager.trackScheduler.playing) {
            context.send().error("I'm not playing anything right now").queue()
            return
        }
        if (context.data.musicManager.adminOnly) {
            forceSkip(context)
            return
        }
        var shouldHalt = false
        cmdContext.has<String>("action") { action ->
            if (action.equals("force", true)) {
                if (context.author.getClearance(context.guild).value >= Clearance.SERVER_ADMINISTRATOR.value) {
                    forceSkip(context)
                    shouldHalt = true
                }
            }
        }
        if(shouldHalt)
            return
        val skipIn = skipCooldown[context.author.idLong] ?: 0
        if (System.currentTimeMillis() < skipIn || skipIn == -1L) {
            val phrase = if (skipIn == -1L) "Try again after your current poll expires!" else "Try again in " +
                    localizeTime(((skipIn - System.currentTimeMillis()) / 1000).toInt())
            context.send().error("You are doing that too much! **$phrase**").queue()
            return
        }
        // Start a vote
        val currentlyPlaying = context.data.musicManager.trackScheduler.nowPlaying
        val musicData = context.data.getMusicData()
        context.send().embed("Music") {
            setColor(Color.CYAN)
            setDescription(buildString {
                appendln(b(context.author.name))
                append(" has voted to **SKIP** the current track! ")
                appendln("React with :thumbsup: or :thumbsdown: to vote")
                appendln("Whichever has the most votes in ${localizeTime(musicData.skipTimer)} wins")
            })
        }.rest().queue { m ->
            skipCooldown[context.author.idLong] = -1
            m.addReaction("👍").queue()
            m.addReaction("👎").queue()

            m.editMessage(embed("Music") {
                setDescription("Voting has ended! Check newer messages for results")
                setColor(Color.RED)
            }.build()).queueAfter(musicData.skipTimer.toLong(), TimeUnit.SECONDS) {

                if (context.data.musicManager.trackScheduler.nowPlaying?.identifier != currentlyPlaying?.identifier) {
                    context.send().embed("Music") {
                        setColor(Color.CYAN)
                        setDescription("Song has changed, canceling vote")
                    }.rest().queue()
                    return@queueAfter
                }

                var skip = 0
                var stay = 0

                it.reactions.forEach {
                    if (it.emote.name == "👎") stay = it.count - 1
                    if (it.emote.name == "👍") skip = it.count - 1
                }

                context.send().embed("Music") {
                    setColor(Color.CYAN)
                    setDescription(buildString {
                        if (musicData.skipCooldown > 0)
                            skipCooldown[context.author.idLong] = System.currentTimeMillis() + (musicData.skipCooldown * 1000)
                        else
                            skipCooldown.remove(context.author.idLong)
                        if (skip > stay) {
                            appendln("The vote has passed! Playing next song.")
                            context.data.musicManager.trackScheduler.playNext()
                        } else {
                            appendln("The vote has failed! The song will stay.")
                        }
                    })
                }.rest().queue()
            }
        }
    }

    private fun forceSkip(context: Context) {
        context.send().embed("Music") {
            setColor(Color.CYAN)
            setDescription("Playing next song...")
        }.rest().queue()
        context.data.musicManager.trackScheduler.playNext()
    }
}