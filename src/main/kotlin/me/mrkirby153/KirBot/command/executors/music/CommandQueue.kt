package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.embed.u
import me.mrkirby153.KirBot.utils.getClearance
import net.dv8tion.jda.core.entities.MessageEmbed
import java.awt.Color

@Command(name = "queue", description = "Shows the current play queue", category = "Music")
class CommandQueue : MusicCommand() {
    override fun exec(context: Context, args: Array<String>) {
        if(args.isNotEmpty()){
            if(args[0] == "clear"){
                if(context.author.getClearance(guild).value >= Clearance.SERVER_ADMINISTRATOR.value) {
                    serverData.musicManager.trackScheduler.queue.clear()
                    context.send().embed("Queue") {
                        setColor(Color.CYAN)
                        setDescription("Queue Cleared!")
                    }.rest().queue()
                } else {
                    context.send().error("You do not have permission to perform that command!").queue()
                }
                return
            }
        }
        val queue = serverData.musicManager.trackScheduler.queue
        val duration = serverData.musicManager.trackScheduler.queueLength()

        val np = serverData.musicManager.trackScheduler.nowPlaying

        var displayedTracks = 0

        // Display the next five songs
        context.send().embed("Music Queue") {
            setColor(Color.cyan)
            np?.let {
                field("Now Playing", false, " ${formatDuration((np.duration / 1000).toInt())} __**[${np.info.title}](${np.info.uri})**__")
            }

            field("Queue", false) {
                buildString {
                    if (queue.isEmpty()) {
                        append(u("Empty Queue."))
                    } else for (track in queue) {
                        displayedTracks++
                        appendln("**$displayedTracks.** [${formatDuration((track.duration / 1000).toInt())}] __[${track.info.title}](${track.info.uri})__")
                        if (length >= MessageEmbed.VALUE_MAX_LENGTH - 200 && queue.size - displayedTracks > 0) {
                            append(" And **${queue.size - displayedTracks}** more!")
                            break
                        }
                    }
                }
            }

            field("Size", true, queue.size)
            field("Duration", true, formatDuration(duration))
            field("", true, "Click Here to view the full queue" link "https://kirbot.mrkirby153.tk/${context.guild.id}/queue")
        }.rest().queue()
    }


    private fun formatDuration(seconds: Int): String {
        val mins = seconds / 60
        val secs = seconds - (mins * 60)

        return buildString {
            if (mins < 10) {
                append("0$mins")
            } else {
                append(mins)
            }
            append(":")
            if (secs < 10) {
                append("0$secs")
            } else {
                append(secs)
            }
        }
    }
}