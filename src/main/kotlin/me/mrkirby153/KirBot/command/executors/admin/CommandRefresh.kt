package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.kirbotGuild

@Command(name = "refresh", clearance = CLEARANCE_ADMIN)
@CommandDescription("Updates the currently running configuration with the database")
class CommandRefresh : BaseCommand(CommandCategory.ADMIN) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        context.guild.kirbotGuild.sync(true)
        context.success()
    }
}