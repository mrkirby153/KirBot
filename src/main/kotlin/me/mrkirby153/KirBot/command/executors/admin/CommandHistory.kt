package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.HttpUtils
import net.dv8tion.jda.core.entities.MessageHistory
import net.dv8tion.jda.core.entities.TextChannel
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import org.json.JSONTokener
import java.time.format.DateTimeFormatter

class CommandHistory : CmdExecutor() {

    val HASTEBIN_URL = "https://paste.mrkirby153.tk/"

    override fun execute(context: Context, cmdContext: CommandContext) {
        context.send().info("Creating channel history, this may take a while...").complete()
        val msgHistory = getHistory(context.channel as TextChannel).retrievedHistory.toTypedArray().reversed()
        val msgHistoryText = buildString {
            msgHistory.forEach {
                append(it.creationTime.format(DateTimeFormatter.ofPattern("YYY-MM-dd HH:MM:ss")))
                append("| ${it.author.name}: ${it.content}")
                if (it.embeds.size != 0) {
                    append("\n\t\t" + buildString {
                        it.embeds.forEach {
                            append("[${it.title}]")
                            if (it.description != null)
                                append(": ${it.description}")
                        }
                    })
                }
                append("\n")
            }
        }
        // Send data to hastebin and output success to channel.
        context.send().success(paste(msgHistoryText)).queue()
    }

    fun paste(content: String): String {
        val body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), content)

        val request = Request.Builder().url("$HASTEBIN_URL/documents").post(body).build()
        val response = HttpUtils.CLIENT.newCall(request).execute()
        val json  = JSONObject(JSONTokener(response.body()?.byteStream()))
        return "$HASTEBIN_URL${json.optString("key")}.txt"
    }

    fun getHistory(channel: TextChannel): MessageHistory {
        val history = channel.history
        var lastSize: Int
        do {
            lastSize = history.size()
            history.retrievePast(100).complete()
        } while(lastSize != history.size())
        // Got entire channel history
        println("History retrieved!")
        println("Total history size: ${history.size()}")
        return history
    }

    private data class JsonResponse(val key: String)
}