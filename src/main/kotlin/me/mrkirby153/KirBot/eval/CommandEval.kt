package me.mrkirby153.KirBot.eval

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.GREEN_TICK
import net.dv8tion.jda.api.requests.RestAction
import org.json.JSONObject
import org.json.JSONTokener
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL

const val CHECKBOX = "\u2705"
const val ARROWS = "\uD83D\uDD04"


class CommandEval {

    @Command(name = "eval", arguments = ["<eval:string...>"])
    @AdminCommand
    fun execute(context: Context, cmdContext: CommandContext) {
        val toEval = cmdContext.get<String>("eval") ?: return

        val shortcuts = mutableMapOf<String, Any>()

        shortcuts["jda"] = context.guild.jda
        shortcuts["guild"] = context.guild
        shortcuts["channel"] = context.channel
        shortcuts["context"] = context
        shortcuts["message"] = context.message
        shortcuts["msg"] = context.message
        shortcuts["me"] = context.author
        shortcuts["bot"] = Bot

        context.message.addReaction(ARROWS).queue()

        val result = Engine.eval(shortcuts, Engine.defaultImports, 10, toEval)
        context.message.addReaction(GREEN_TICK.emote!!).queue()
        if (result.first is RestAction<*>) {
            Bot.LOG.debug("Eval returned rest action, queueing")
            (result.first as RestAction<*>).queue()
            return
        }
        val stdOut = result.second
        val stdErr = result.third

        val toSend = buildString {
            if (result.first != null)
                append("```${result.first}```")
            if (stdOut.isNotEmpty())
                append("**Output**```$stdOut```")
            if (stdErr.isNotEmpty())
                append("**Errors:**\n```$stdErr```")
        }

        if (toSend.length >= 2000) {
            context.channel.sendMessage(
                    ":timer: Output is too long. Uploading to hastebin").queue { m ->
                val url = URL("https://paste.mrkirby153.com/documents")
                val con = url.openConnection() as HttpURLConnection
                con.requestMethod = "POST"
                con.doInput = true
                con.doOutput = true
                con.setRequestProperty("User-Agent",
                        "KirBot/${Bot.constants.getProperty("bot-version")}")

                val os = DataOutputStream(con.outputStream)
                os.writeBytes(buildString {
                    append("Results:")
                    if (result.first != null) {
                        append("\n${result.first}")
                    } else {
                        append(" None")
                    }

                    append("\n\n\n")
                    append("Output: ")
                    if (result.second.isNotEmpty()) {
                        append("\n${result.second}")
                    } else {
                        append("No Std. Output")
                    }

                    append("\n\n\n")
                    append("Errors: ")
                    if (result.third.isNotEmpty()) {
                        append("\n{${result.third}")
                    } else {
                        append("No Errors")
                    }
                })
                os.flush()
                os.close()

                val json = JSONObject(JSONTokener(con.inputStream))
                con.disconnect()

                m.editMessage("https://paste.mrkirby153.com/${json["key"]}.txt").queue()
            }
        } else {
            if (!toSend.isEmpty())
                context.channel.sendMessage(toSend).queue()
        }
    }
}