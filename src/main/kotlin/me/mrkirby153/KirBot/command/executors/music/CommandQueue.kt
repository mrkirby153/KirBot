package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.modules.music.MusicBaseCommand
import me.mrkirby153.KirBot.modules.music.MusicManager
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.botUrl
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.mdEscape
import net.dv8tion.jda.core.Permission
import java.util.Random
import kotlin.math.roundToInt

@Command(name = "queue,np,nowplaying", arguments = ["[option:string]"],
        permissions = [Permission.MESSAGE_EMBED_LINKS])
class CommandQueue : MusicBaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext, manager: MusicManager) {
        if (cmdContext.has("option")) {
            when (cmdContext.get<String>("option")!!.toLowerCase()) {
                "clear" -> {
                    manager.queue.clear()
                    context.send().success("Queue cleared!").queue()
                }
                "shuffle" -> {
                    val queue = manager.queue.toMutableList()
                    val random = Random()
                    manager.queue.clear()
                    while (queue.isNotEmpty()) {
                        val element: MusicManager.QueuedSong = queue[random.nextInt(queue.size)]
                        manager.queue.add(element)
                        queue.remove(element)
                    }
                    context.send().success("Queue shuffled!").queue()
                }
            }
            return
        }
        if (manager.nowPlaying == null && manager.queue.isEmpty()) {
            context.send().embed("Now Playing") {
                description { +"Nothing is playing right now" }
                timestamp {
                    now()
                }
            }.rest().queue()
            return
        }
        context.send().embed {
            val nowPlaying = manager.nowPlaying ?: return@embed
            if (nowPlaying.info.uri.contains("youtu")) {
                thumbnail = "https://i.ytimg.com/vi/${nowPlaying.info.identifier}/default.jpg"
            }
            description {
                +("**Music Queue :musical_note:**" link manager.nowPlaying!!.info.uri)
                +"\n\n__Now Playing__"
                +"\n\n"
                if (manager.playing)
                    +":loud_sound: "
                else
                    +":speaker: "
                val position = "${MusicManager.parseMS(nowPlaying.position)}/${MusicManager.parseMS(
                        nowPlaying.info.length)}"
                +nowPlaying.info.title.mdEscape() link nowPlaying.info.uri
                +"\n\n :rewind: ${buildProgressBar(12, nowPlaying.position, nowPlaying.duration)} :fast_forward: ($position)"
                +"\n\n:arrow_down_small: __Up Next__ :arrow_down_small:"
                +"\n\n"
                if (manager.queue.size == 0)
                    +"Nothing"
                else
                    manager.queue.forEachIndexed { index, (track) ->
                        if (length < 1500)
                            appendln(
                                    " " + (index + 1) + ". " + (track.info.title link track.info.uri) + " (${MusicManager.parseMS(
                                            track.duration)})")
                    }
                +"\n\n"
                +("**View The Full Queue**" link botUrl("server/${context.guild.id}/queue"))
            }
            timestamp {
                now()
            }
            footer {
                text {
                    var duration = 0L
                    manager.queue.forEach {
                        duration += it.track.duration
                    }
                    duration += nowPlaying.duration
                    append("\n\n${MusicManager.parseMS(
                            duration)} | ${manager.queue.size + 1} songs")
                }
            }
        }.rest().queue()
    }

    private fun buildProgressBar(steps: Int, current: Long, max: Long, bar: String = "▬",
                                 dot: String = "\uD83D\uDD18"): String {
        val percent = current.toDouble() / max

        val pos = Math.min(Math.max((steps * percent).roundToInt(), 1), steps)
        return buildString {
            for (i in 1 until (steps+1)) {
                if (i == pos)
                    append(dot)
                else
                    append(bar)
            }
        }
    }
}