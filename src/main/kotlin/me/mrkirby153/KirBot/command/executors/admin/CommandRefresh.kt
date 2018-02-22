package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.kirbotGuild

@Command(name = "refresh", clearance = Clearance.SERVER_ADMINISTRATOR)
class CommandRefresh : BaseCommand(CommandCategory.ADMIN) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        context.guild.kirbotGuild.sync()
        context.success()
    }
}