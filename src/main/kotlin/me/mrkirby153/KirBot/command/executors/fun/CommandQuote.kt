package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.api.Quote
import me.mrkirby153.KirBot.utils.Context

@Command("quote")
class CommandQuote : BaseCommand(false, CommandCategory.FUN, Arguments.number("id")){
    override fun execute(context: Context, cmdContext: CommandContext) {
        val quote = cmdContext.get<Double>("id")
        Quote.get(quote.toString()).queue { q ->
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