package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.utils.Context

class CommandRefresh : CmdExecutor() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val arg = cmdContext.string("item") ?: "$$$$$$" // Random string not likely to be used

        when (arg) {
            "channels" -> {
                PanelAPI.updateChannels(context.guild)
                context.send().success("Updated server channels!").queue()
            }
            else -> {
                context.send().error("Not a valid item to refresh!")
            }
        }
    }
}