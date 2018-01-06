package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.RequiresClearance
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.hide
import net.dv8tion.jda.core.entities.TextChannel
import java.awt.Color

@Command("hide")
@RequiresClearance(Clearance.BOT_MANAGER)
class CommandHideChannel : BaseCommand(false, CommandCategory.MODERATION){
    override fun execute(context: Context, cmdContext: CommandContext) {
        val channel = (context.channel as? TextChannel) ?: return
        channel.hide()
        context.send().success("Channel hidden!").complete()
        context.data.logManager.genericLog("Channel Hidden", "${context.author.asMention} has hidden ${context.textChannel.asMention}", Color.GREEN, context.author)
    }
}