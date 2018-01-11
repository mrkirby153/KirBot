package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.RequiresClearance
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context

@Command("updateNames")
@RequiresClearance(Clearance.BOT_MANAGER)
class UpdateNicknames : BaseCommand() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        throw CommandException("Temporarily Disabled...")
    }
}