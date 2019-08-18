package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.backfill.RecoveryManager
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.kcutils.Time

class CommandRecovery {

    @AdminCommand
    @Command(name = "global", arguments = ["<duration:string>", "[pool:int]"], parent = "recover")
    fun globalRecovery(context: Context, cmdContext: CommandContext) {
        val duration = try {
            Time.parse(cmdContext.getNotNull("duration"))
        } catch (e: IllegalArgumentException) {
            throw CommandException(e.message)
        }
        val pool = cmdContext.get<Int>("pool") ?: 10
        context.channel.sendMessage("Recovery started with $pool threads").complete()
        val ar = RecoveryManager.globalRecovery(duration, pool)
        val msg = context.channel.sendMessage(
                "Recovery status: ${ar.completed.size}/${ar.channels.size}").complete()
        val start = System.currentTimeMillis()
        Bot.scheduler.submit {
            val end: Long
            while (true) {
                msg.editMessage("Recovery status: ${ar.completed.size}/${ar.channels.size}").queue()
                if (ar.completed.size == ar.channels.size) {
                    end = System.currentTimeMillis()
                    break
                }
                Thread.sleep(5000)
            }
            context.channel.sendMessage("Recovery completed! (Took ${Time.format(1,
                    end - start)} and recovered ${ar.recoveredMessages} messages)").queue()
        }
    }
}