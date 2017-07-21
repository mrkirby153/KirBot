package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.entities.TextChannel

class CommandDisableSpamFilter: CmdExecutor() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        context.shard.getServerData(context.guild).disableSpamFilter(context.channel as TextChannel)
        context.send().success("Disabled the spam filter for `1.0 hours`").queue()
    }
}