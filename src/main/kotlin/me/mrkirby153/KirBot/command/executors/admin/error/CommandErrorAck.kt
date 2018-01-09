package me.mrkirby153.KirBot.command.executors.admin.error

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.RequiresClearance
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.logger.ErrorLogger
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import java.util.concurrent.TimeUnit

@Command("eack")
@RequiresClearance(Clearance.BOT_OWNER)
class CommandErrorAck : BaseCommand(false, CommandCategory.UNCATEGORIZED, Arguments.string("id")){
    override fun execute(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<String>("id") ?: throw CommandException("Specify an ID")
        if(id.equals("all", true)){
            ErrorLogger.ackAll()
            context.success()
            context.deleteAfter(10, TimeUnit.SECONDS)
            return
        }

        val trace = ErrorLogger.getTrace(id) ?: throw CommandException(
                "There is no error with that ID!")
        ErrorLogger.acknowledge(id)
        context.deleteAfter(10, TimeUnit.SECONDS)
    }
}