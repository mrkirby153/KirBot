package me.mrkirby153.KirBot.eval

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.embed.u
import net.dv8tion.jda.core.requests.RestAction
import org.json.JSONObject
import org.json.JSONTokener
import java.awt.Color
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

const val CHECKBOX = "\u2705"
const val ARROWS = "\uD83D\uDD04"

class CommandEval : CmdExecutor() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val toEval = cmdContext.get<String>("eval") ?: return

        val shortcuts = mutableMapOf<String, Any>()

        shortcuts["jda"] = context.guild.jda
        shortcuts["guild"] = context.guild
        shortcuts["channel"] = context.channel
        shortcuts["context"] = context
        shortcuts["message"] = context.message
        shortcuts["msg"] = context.message
        shortcuts["me"] = context.author

        context.message.addReaction(ARROWS).queue()

        val result = Engine.eval(shortcuts, Engine.defaultImports, 10, toEval)

        if (result.first is RestAction<*>)
            (result.first as RestAction<*>).queue()
        if (result.second.isEmpty() && result.third.isEmpty()) {
            context.message.addReaction(CHECKBOX).queue()
            return
        }
        val builder = embed("Run Results") {
            color = if (result.third.isEmpty()) Color.GREEN else Color.RED
            description {
                +u("Results\n")
                if (result.first == null)
                    +"No results \n"
                else
                    +result.first.toString()
                +"\n\n__Output:__\n"
                if (!result.second.isEmpty())
                    +"```\n${result.second}```"
                else
                    +"No Std. Output\n"
                +"\n\n__Errors__\n"
                if (!result.third.isEmpty())
                    +"```\n${result.third}```"
                else
                    +"No errors."
            }
        }
        context.message.addReaction(CHECKBOX).queue()
        if (builder.getDescription().length >= 2048) {
            // Too large, uploading to Hastebin
            context.channel.sendMessage("Output is too long, sending to Hastebin...").queue({ msg ->
                Bot.scheduler.schedule({
                    val url = URL("https://paste.mrkirby153.com/documents")
                    val con = url.openConnection() as HttpURLConnection
                    con.requestMethod = "POST"
                    con.doInput = true
                    con.doOutput = true
                    con.setRequestProperty("User-Agent", "KirBot/${Bot.constants.getProperty("bot-version")}")

                    val os = DataOutputStream(con.outputStream)
                    os.writeBytes(buildString {
                        append("Results:")
                        if(result.first != null) {
                            append("\n${result.first}")
                        } else {
                            append(" None")
                        }

                        append("\n\n\n")
                        append("Output: ")
                        if(result.second.isNotEmpty()){
                            append("\n${result.second}")
                        } else {
                            append("No Std. Output")
                        }

                        append("\n\n\n")
                        append("Errors: ")
                        if(result.third.isNotEmpty()) {
                            append("\n{${result.third}")
                        } else {
                            append("No Errors")
                        }
                    })
                    os.flush()
                    os.close()

                    val json = JSONObject(JSONTokener(con.inputStream))
                    con.disconnect()

                    msg.editMessage("https://paste.mrkirby153.com/${json["key"]}.txt").queue()
                }, 0, TimeUnit.MILLISECONDS)
            })

        } else {
            context.channel.sendMessage(builder.build()).queue()
        }
    }
}