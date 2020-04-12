package me.mrkirby153.KirBot.command.control.error

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.logger.ErrorLogger
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import java.util.concurrent.TimeUnit


class CommandErrorAck {

    @Command(name = "eack", arguments = ["<id:string>"])
    @AdminCommand
    fun execute(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<String>("id") ?: throw CommandException("Specify an ID")
        if (id.equals("all", true)) {
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