package me.mrkirby153.KirBot.command.executors.admin.error

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.logger.ErrorLogger
import me.mrkirby153.KirBot.user.CLEARANCE_GLOBAL_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import java.util.concurrent.TimeUnit

@Command(name = "eack", arguments = ["<id:string>"], clearance = CLEARANCE_GLOBAL_ADMIN)
class CommandErrorAck : BaseCommand(false, CommandCategory.UNCATEGORIZED){
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