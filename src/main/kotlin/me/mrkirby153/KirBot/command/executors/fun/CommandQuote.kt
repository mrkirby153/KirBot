package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.Quote
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Quotes
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.botUrl
import me.mrkirby153.KirBot.utils.escapeMentions
import me.mrkirby153.KirBot.utils.nameAndDiscrim

@Command(name = "quote", arguments = ["[id:int]"])
class CommandQuote : BaseCommand(false, CommandCategory.FUN) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!cmdContext.has("id")) {
            context.channel.sendMessage(
                    ":left_speech_bubble: **Quotes**\n\n React to any message with :left_speech_bubble: " +
                            "to quote it. You can retrieve a quote by its id by typing `${cmdPrefix}quote <id>`. To view a full list of quotes, type `${cmdPrefix}quotes`").queue()
            return
        }
        val quote = cmdContext.get<Int>("id")!!
        if(quote <= 0)
            throw CommandException("Specify a number greater than 0")
        val q = Model.first(Quote::class.java,
                quote.toString()) ?: throw CommandException("That quote doesn't exist")
        if (q.serverId != context.guild.id) {
            throw CommandException("That quote doesn't exist")
        }
        context.channel.sendMessage("\"${q.content.escapeMentions()}\" \n - ${q.user}").queue()
    }
}

@Command(name = "quotes")
class CommandQuotes : BaseCommand(false, CommandCategory.FUN) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val quoteCount = Model.get(Quote::class.java, Pair("server_id", context.guild.id)).size
        context.channel.sendMessage(
                ":left_speech_bubble: **Quotes**\n\n Total: $quoteCount \n\n Full List: " + botUrl(
                        "${context.guild.id}/quotes")).queue()
    }

    @Command(name = "block", arguments = ["<user:snowflake>"], clearance = CLEARANCE_MOD)
    fun block(context: Context, cmdContext: CommandContext) {
        ModuleManager[Quotes::class.java].blockUser(context.kirbotGuild,
                cmdContext.get<String>("user")!!)
        context.send().success("Blocked ${Bot.shardManager.getUser(
                cmdContext.get<String>("user")!!)?.nameAndDiscrim ?: cmdContext.get(
                "user")!!} from quoting").queue()
    }
    @Command(name = "unblock", arguments = ["<user:snowflake>"], clearance = CLEARANCE_MOD)
    fun unblock(context: Context, cmdContext: CommandContext) {
        ModuleManager[Quotes::class.java].unblockUser(context.kirbotGuild,
                cmdContext.get<String>("user")!!)
        context.send().success("Unblocked ${Bot.shardManager.getUser(
                cmdContext.get<String>("user")!!)?.nameAndDiscrim ?: cmdContext.get(
                "user")!!} from quoting").queue()
    }
}