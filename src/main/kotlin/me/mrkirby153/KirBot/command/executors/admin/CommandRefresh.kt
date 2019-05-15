package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.kirbotGuild


class CommandRefresh {

    @Command(name = "refresh", clearance = CLEARANCE_ADMIN, category = CommandCategory.ADMIN)
    @CommandDescription("Updates the currently running configuration with the database")
    fun execute(context: Context, cmdContext: CommandContext) {
        context.guild.kirbotGuild.sync()
        context.success()
    }
}