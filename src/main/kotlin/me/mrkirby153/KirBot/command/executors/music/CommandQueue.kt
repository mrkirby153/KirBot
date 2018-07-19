package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.music.MusicManager
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.botUrl
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.mdEscape
import net.dv8tion.jda.core.Permission
import java.util.Random

@Command(name = "queue,np,nowplaying", arguments = ["[option:string]"],
        permissions = [Permission.MESSAGE_EMBED_LINKS])
class CommandQueue : BaseCommand(CommandCategory.MUSIC) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!context.kirbotGuild.musicManager.settings.enabled) {
            return
        } else {
            Bot.LOG.debug("Music is disabled in ${context.guild.id}, ignoring")
        }
        if (cmdContext.has("option")) {
            when (cmdContext.get<String>("option")!!.toLowerCase()) {
                "clear" -> {
                    context.kirbotGuild.musicManager.queue.clear()
                    context.send().success("Queue cleared!").queue()
                }
                "shuffle" -> {
                    val queue = context.kirbotGuild.musicManager.queue.toTypedArray().toMutableList()
                    val random = Random()
                    context.kirbotGuild.musicManager.queue.clear()
                    while (queue.isNotEmpty()) {
                        val element: MusicManager.QueuedSong = queue[random.nextInt(queue.size)]
                        context.kirbotGuild.musicManager.queue.add(element)
                        queue.remove(element)
                    }
                    context.send().success("Queue shuffled!").queue()
                }
            }
            context.kirbotGuild.musicManager.updateQueue()
            return
        }
        val musicManager = context.kirbotGuild.musicManager
        if (musicManager.nowPlaying == null && musicManager.queue.isEmpty()) {
            context.channel.sendMessage(":x: Nothing is playing right now!").queue()
            return
        }
        context.send().embed {
            val nowPlaying = musicManager.nowPlaying ?: return@embed
            if (nowPlaying.info.uri.contains("youtu")) {
                thumbnail = "https://i.ytimg.com/vi/${nowPlaying.info.identifier}/default.jpg"
            }
            description {
                +("**Music Queue :musical_note:**" link musicManager.nowPlaying!!.info.uri)
                +"\n\n__Now Playing__"
                +"\n\n"
                if (musicManager.playing)
                    +":loud_sound:"
                else
                    +":speaker:"
                val position = "${MusicManager.parseMS(nowPlaying.position)}/${MusicManager.parseMS(
                        nowPlaying.info.length)}"
                +nowPlaying.info.title.mdEscape() link nowPlaying.info.uri
                +"($position)"
                +"\n\n:arrow_down_small: __Up Next__ :arrow_down_small:"
                +"\n\n"
                if (musicManager.queue.size == 0)
                    +"Nothing"
                else
                    musicManager.queue.forEachIndexed { index, (track) ->
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
                    musicManager.queue.forEach {
                        duration += it.track.duration
                    }
                    duration += nowPlaying.duration
                    append("\n\n${MusicManager.parseMS(
                            duration)} | ${musicManager.queue.size + 1} songs")
                }
            }
        }.rest().queue()
    }
}