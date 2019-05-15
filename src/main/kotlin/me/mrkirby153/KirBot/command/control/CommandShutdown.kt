package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context


class CommandShutdown{

    @Command(name = "shutdown")
    @AdminCommand
    fun execute(context: Context, cmdContext: CommandContext) {
        context.send().success("Shutting down...", true).queue{ Bot.stop() }
    }
}