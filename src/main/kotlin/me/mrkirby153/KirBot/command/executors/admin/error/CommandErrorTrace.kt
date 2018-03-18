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

@Command(name = "etrace", clearance = CLEARANCE_GLOBAL_ADMIN, arguments = ["<id:string>"])
class CommandErrorTrace :
        BaseCommand(false, CommandCategory.UNCATEGORIZED) {

    override fun execute(context: Context, cmdContext: CommandContext) {
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