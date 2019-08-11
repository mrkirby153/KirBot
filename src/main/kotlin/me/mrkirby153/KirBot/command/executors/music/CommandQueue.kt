package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.music.MusicManager
import me.mrkirby153.KirBot.modules.music.MusicModule
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.SettingsRepository
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.escapeMarkdown
import net.dv8tion.jda.api.Permission
import java.util.Random
import kotlin.math.roundToInt


class CommandQueue {

    @Command(name = "queue", arguments = ["[option:string]"], aliases = ["np", "nowplaying"],
            permissions = [Permission.MESSAGE_EMBED_LINKS], category = CommandCategory.MUSIC)
    @CommandDescription("Shows the current queue")
    fun execute(context: Context, cmdContext: CommandContext) {
        val manager = ModuleManager[MusicModule::class.java].getManager(context.guild)
        if (SettingsRepository.get(context.guild, "music_enabled", "0") == "0")
            return
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
                +nowPlaying.info.title.escapeMarkdown() link nowPlaying.info.uri
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