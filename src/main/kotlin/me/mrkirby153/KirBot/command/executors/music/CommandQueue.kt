package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.music.MusicManager
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.link

class CommandQueue : CmdExecutor() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val musicManager = context.data.musicManager
        context.send().embed {
            setDescription(buildString {
                if (musicManager.nowPlaying != null)
                    append("**Music Queue**" link musicManager.nowPlaying!!.info.uri)
                else {
                    append("**Music Queue**\n\n")
                    append("Nothing is playing right now")
                    return@buildString
                }
                append("\n\n__Now Playing__")
                append("\n\n")
                val nowPlaying = musicManager.nowPlaying ?: return@buildString
                val d = "${MusicManager.parseMS(nowPlaying.position)}/${MusicManager.parseMS(nowPlaying.info.length)}"
                append((nowPlaying.info.title link nowPlaying.info.uri) + " ($d)")
                append("\n\n__Up Next__")
                append("\n\n")
                if (musicManager.queue.size == 0)
                    append("Nothing")
                else
                    musicManager.queue.forEach {
                        append(" " + (it.track.info.title link it.track.info.uri) + " (${MusicManager.parseMS(it.track.duration)})")
                        if (length > 1500)
                            return@forEach
                    }
                append("\n\n")
                append("**View The Full Queue**" link "https://kirbot.mrkirby153.com/${context.guild.id}/queue")

                var duration = 0L
                musicManager.queue.forEach {
                    duration += it.track.duration
                }
                duration += nowPlaying.duration
                append("\n\n**${MusicManager.parseMS(duration)} | ${musicManager.queue.size + 1} songs**")
            })
        }.rest().queue()
    }
}