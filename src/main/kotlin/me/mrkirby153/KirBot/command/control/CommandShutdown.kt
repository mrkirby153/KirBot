package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context

@Command(name = "shutdown", control = true)
class CommandShutdown : BaseCommand(false, CommandCategory.ADMIN) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        context.send().success("Shutting down...", true).queue{ Bot.stop() }
    }
}