package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.RequiresClearance
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.entities.TextChannel

@Command("spamFilter")
@RequiresClearance(Clearance.BOT_MANAGER)
class CommandDisableSpamFilter : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        context.shard.getServerData(context.guild).disableSpamFilter(context.channel as TextChannel)
        context.send().success("Disabled the spam filter for `1.0 hours`").queue()
    }
}