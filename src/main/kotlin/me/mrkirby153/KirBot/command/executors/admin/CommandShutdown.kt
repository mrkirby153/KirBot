package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import java.awt.Color

class CommandShutdown : CmdExecutor() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        context.send().embed("Shut Down") {
            color = Color.RED
            description { +"Good bye :wave:" }
        }.rest().queue{ Bot.stop() }
    }
}