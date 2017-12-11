package me.mrkirby153.KirBot.eval

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.embed.u
import net.dv8tion.jda.core.requests.RestAction
import java.awt.Color

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
        context.channel.sendMessage(builder.build()).queue()
    }
}