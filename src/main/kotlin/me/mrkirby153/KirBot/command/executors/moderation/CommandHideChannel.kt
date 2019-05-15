package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.hide
import me.mrkirby153.KirBot.utils.unhide
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel


class CommandHideChannel {
    @Command(name = "hide", clearance = CLEARANCE_ADMIN, permissions = [Permission.MANAGE_CHANNEL],
            category = CommandCategory.MODERATION)
    @LogInModlogs
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val channel = (context.channel as? TextChannel) ?: return
        channel.hide()
        context.send().success("Channel hidden!").complete()
    }
}


class UnhideChannel {

    @Command(name = "unhide", clearance = CLEARANCE_ADMIN,
            permissions = [Permission.MANAGE_CHANNEL], category = CommandCategory.MODERATION)
    @LogInModlogs
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val channel = (context.channel as? TextChannel) ?: return
        channel.unhide()
        context.send().success("Channel unhidden").complete()
    }

}