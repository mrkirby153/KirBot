package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.hide
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.KirBot.utils.unhide
import net.dv8tion.jda.core.entities.TextChannel

@Command(name = "hide", clearance = Clearance.BOT_MANAGER)
class CommandHideChannel : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val channel = (context.channel as? TextChannel) ?: return
        channel.hide()
        context.send().success("Channel hidden!").complete()
        context.kirbotGuild.logManager.genericLog(":no_entry:",
                "${context.author.nameAndDiscrim} has hidden #${channel.name}")
    }
}

@Command(name = "unhide", clearance = Clearance.BOT_MANAGER)
class UnhideChannel : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val channel = (context.channel as? TextChannel) ?: return

        channel.unhide()
        context.send().success("Channel unhidden").complete()
        context.kirbotGuild.logManager.genericLog(":ballot_box_with_check:",
                "${context.author.nameAndDiscrim} has unhidden #${channel.name}")
    }

}