package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.kcutils.Time

@Command(name = "ping")
class CommandPing : BaseCommand(false) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val start = System.currentTimeMillis()
        context.channel.sendMessage("Testing ping...").queue {
            val stop = System.currentTimeMillis()
            it.editMessage(
                    ":clock12: Ping: `${Time.format(1, stop - start, Time.TimeUnit.FIT)}`").queue()
        }
    }
}