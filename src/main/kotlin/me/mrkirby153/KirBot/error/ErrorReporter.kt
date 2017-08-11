package me.mrkirby153.KirBot.error

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.embed.embed
import java.awt.Color
import java.io.File
import java.text.SimpleDateFormat

object ErrorReporter {

    init {
        Thread.setDefaultUncaughtExceptionHandler(UncaughtErrorReporter())
    }

    var channel = ""
    var guild = ""

    fun reportError(e: Throwable, thread: Thread? = null) {
        e.printStackTrace()
        val temp = File.createTempFile("kirbot-error", ".log")
        val writer = temp.printWriter()
        e.printStackTrace(writer)
        writer.flush()
        if(channel.isEmpty() || guild.isEmpty())
            return
        Bot.getGuild(guild)?.getTextChannelById(channel)?.apply {
            sendMessage(embed("Exception caught") {
                setColor(Color.RED)
                setDescription("An uncaught exception occurred")
                field("Type", false, e.javaClass)
                field("Message", true, e.message)
                field("Thread", true, buildString {
                    if (thread != null)
                        append(thread.name)
                    else
                        append(Thread.currentThread().name)
                })
                field("Time", true, SimpleDateFormat("YYY-MM-dd HH:mm:ss").format(System.currentTimeMillis()))
            }.build()).queue {
                sendFile(temp, temp.name, null).queue {
                    writer.close()
                    if (!temp.delete())
                        temp.deleteOnExit()
                }
            }
        }
    }
}