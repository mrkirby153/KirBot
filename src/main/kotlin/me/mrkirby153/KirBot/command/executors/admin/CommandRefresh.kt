package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.kirbotGuild
import net.dv8tion.jda.api.Permission


class CommandRefresh {

    @Command(name = "refresh", clearance = CLEARANCE_ADMIN, category = CommandCategory.ADMIN)
    @CommandDescription("Updates the currently running configuration with the database")
    @LogInModlogs
    fun execute(context: Context, cmdContext: CommandContext) {
        context.guild.kirbotGuild.sync()
        if (context.channel.checkPermissions(Permission.MESSAGE_ADD_REACTION))
            context.success()
        else
            context.send().success("Sync completed!").queue()
    }
}