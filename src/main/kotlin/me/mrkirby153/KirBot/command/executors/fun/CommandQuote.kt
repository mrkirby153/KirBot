package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.Quote
import me.mrkirby153.KirBot.utils.Context

@Command("quote")
class CommandQuote : BaseCommand(false, CommandCategory.FUN, Arguments.number("id")) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val quote = cmdContext.get<Double>("id")
        val q = Model.first(Quote::class.java,
                quote.toString()) ?: throw CommandException("That quote doesn't exist")
        if (q.serverId != context.guild.id) {
            throw CommandException("That quote doesn't exist")
        }
        context.channel.sendMessage("\"${q.content}\" \n - ${q.user}").queue()
    }
}