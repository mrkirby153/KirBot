package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context

@Command(name = "updateNames")
class UpdateNicknames : BaseCommand() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        throw CommandException("Temporarily Disabled...")
    }
}