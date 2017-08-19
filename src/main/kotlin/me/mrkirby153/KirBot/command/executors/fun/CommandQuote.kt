package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.utils.Context

class CommandQuote : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val quote = cmdContext.number("id")?.toInt()
        PanelAPI.getQuote(quote.toString()).queue { q ->
            if (q != null) {
                if(q.server != context.guild.id){
                    return@queue
                }
                context.channel.sendMessage("\"${q.content}\" \n-${q.user}").queue()
            } else {
                context.send().error("That quote does not exist!")
            }
        }
    }
}