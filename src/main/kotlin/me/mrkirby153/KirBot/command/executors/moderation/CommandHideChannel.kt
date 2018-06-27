package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.hide
import me.mrkirby153.KirBot.utils.unhide
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel

@Command(name = "hide", clearance = CLEARANCE_ADMIN)
@LogInModlogs
class CommandHideChannel : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val channel = (context.channel as? TextChannel) ?: return
        if(!context.channel.checkPermissions(Permission.MANAGE_CHANNEL))
            throw CommandException("Cannot modify this channel")
        channel.hide()
        context.send().success("Channel hidden!").complete()
    }
}

@Command(name = "unhide", clearance = CLEARANCE_ADMIN)
@LogInModlogs
class UnhideChannel : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val channel = (context.channel as? TextChannel) ?: return
        if(!context.channel.checkPermissions(Permission.MANAGE_CHANNEL))
            throw CommandException("Cannot modify this channel")
        channel.unhide()
        context.send().success("Channel unhidden").complete()
    }

}