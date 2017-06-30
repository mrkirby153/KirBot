package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.realname.RealnameHandler
import me.mrkirby153.KirBot.utils.Context

class UpdateNicknames : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val handler = RealnameHandler(context.guild, context.data)
        handler.updateNames()
        context.send().success("Real names were refreshed from the database!").queue()
    }
}