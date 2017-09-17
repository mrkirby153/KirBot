package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.music.MusicManager
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.link
import java.util.*

class CommandQueue : CmdExecutor() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        if(cmdContext.get<String>("option") != null){
            when(cmdContext.get<String>("option")!!.toLowerCase()){
                "clear" -> {
                    context.data.musicManager.queue.clear()
                    context.send().success("Queue cleared!").queue()
                }
                "shuffle" -> {
                    val queue = context.data.musicManager.queue.toTypedArray().toMutableList()
                    val random = Random()
                    context.data.musicManager.queue.clear()
                    while(queue.isNotEmpty()){
                        val element: MusicManager.QueuedSong = queue[random.nextInt(queue.size)]
                        context.data.musicManager.queue.add(element)
                        queue.remove(element)
                    }
                    context.send().success("Queue shuffled!").queue()
                }
            }
            return
        }
        val musicManager = context.data.musicManager
        if(musicManager.nowPlaying == null && musicManager.queue.isEmpty()){
            context.channel.sendMessage(":x: Nothing is playing right now!").queue()
            return
        }
        context.send().embed {
            val nowPlaying = musicManager.nowPlaying ?: return@embed
            if (nowPlaying.info.uri.contains("youtu")) {
                thumbnail ="https://i.ytimg.com/vi/${nowPlaying.info.identifier}/default.jpg"
            }
            description {
                +"**Music Queue :muscal_note:**" link musicManager.nowPlaying!!.info.uri
                +"\n\n__Now Playing__"
                +"\n\n"
                if(musicManager.playing)
                    +":loud_sound:"
                else
                    +":speaker"
                val position  = "${MusicManager.parseMS(nowPlaying.position)}/${MusicManager.parseMS(nowPlaying.info.length)}"
                +nowPlaying.info.title link nowPlaying.info.uri
                +"($position)"
                +"\n\n:arrow_down_small: __Up Next__ :arrow_down_small:"
                +"\n\n"
                if(musicManager.queue.size == 0)
                    +"Nothing"
                else
                    musicManager.queue.forEachIndexed { index, (track) ->
                        if(length < 1500)
                            appendln(" " + (index + 1) + ". " + (track.info.title link track.info.uri) + " (${MusicManager.parseMS(track.duration)})")
                    }
                +"\n\n"
                +"**View The Full Queue**" link "https://kirbot.mrkirby153.com/${context.guild.id}/queue"
            }
            footer {
                text {
                    var duration = 0L
                    musicManager.queue.forEach {
                        duration += it.track.duration
                    }
                    duration += nowPlaying.duration
                    append("\n\n**${MusicManager.parseMS(duration)} | ${musicManager.queue.size + 1} songs**")
                }
            }
        }.rest().queue()
    }
}