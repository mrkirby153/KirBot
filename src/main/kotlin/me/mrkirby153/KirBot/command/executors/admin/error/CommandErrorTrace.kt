package me.mrkirby153.KirBot.command.executors.admin.error

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.logger.ErrorLogger
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import java.util.concurrent.TimeUnit


class CommandErrorTrace {

    @Command(name = "etrace", arguments = ["<id:string>"])
    @AdminCommand
    fun execute(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<String>("id") ?: throw CommandException("Specify an ID")

        val trace = ErrorLogger.getTrace(id) ?: throw CommandException(
                "There is no error with that ID!")

        if (trace.length > 1900) {
            // Too large, send file
            context.channel.sendFile(trace.toByteArray(), "error-$id.txt").queue()
        } else {
            context.channel.sendMessage("```$trace```").queue()
        }
        context.deleteAfter(10, TimeUnit.SECONDS)
    }
}