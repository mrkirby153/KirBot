package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.Time

class CommandPing : CmdExecutor() {


    override fun execute(context: Context, cmdContext: CommandContext) {
        val start = System.currentTimeMillis()
        context.channel.sendMessage("Testing ping...").queue {
            val stop = System.currentTimeMillis()
            it.editMessage(
                    ":clock12: Ping: `${Time.format(1, stop - start, Time.TimeUnit.FIT)}`").queue()
        }
    }
}