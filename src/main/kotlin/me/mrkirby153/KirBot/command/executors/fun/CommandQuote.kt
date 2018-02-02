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
import me.mrkirby153.KirBot.utils.botUrl
import me.mrkirby153.KirBot.utils.escapeMentions

@Command("quote")
class CommandQuote : BaseCommand(false, CommandCategory.FUN, Arguments.number("id", false)) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!cmdContext.has("id")) {
            context.channel.sendMessage(
                    ":left_speech_bubble: **Quotes**\n\n React to any message with :left_speech_bubble: " +
                            "to quote it. You can retrieve a quote by its id by typing `${cmdPrefix}quote <id>`. To view a full list of quotes, type `${cmdPrefix}quotes`").queue()
            return
        }
        val quote = cmdContext.get<Double>("id")
        val q = Model.first(Quote::class.java,
                quote.toString()) ?: throw CommandException("That quote doesn't exist")
        if (q.serverId != context.guild.id) {
            throw CommandException("That quote doesn't exist")
        }
        context.channel.sendMessage("\"${q.content.escapeMentions()}\" \n - ${q.user}").queue()
    }
}

@Command("quotes")
class CommandQuotes : BaseCommand(false, CommandCategory.FUN) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val quoteCount = Model.get(Quote::class.java, Pair("server_id", context.guild.id)).size
        context.channel.sendMessage(
                ":left_speech_bubble: **Quotes**\n\n Total: $quoteCount \n\n Full List: " + botUrl(
                        "${context.guild.id}/quotes")).queue()
    }
}