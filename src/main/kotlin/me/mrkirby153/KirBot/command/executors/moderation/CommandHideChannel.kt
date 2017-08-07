package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.hide
import net.dv8tion.jda.core.entities.TextChannel

class CommandHideChannel : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val channel = (context.channel as? TextChannel) ?: return
        channel.hide()
        context.send().success("Channel hidden!").complete()
        context.data.logger.log("Channel Hidden", "${context.author.name} has hidden #${context.channel.name}")
    }

}