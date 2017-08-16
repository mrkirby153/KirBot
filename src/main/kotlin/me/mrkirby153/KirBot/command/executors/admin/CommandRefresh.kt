package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.sync

class CommandRefresh : CmdExecutor() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val arg = cmdContext.string("item") ?: "$$$$$$" // Random string not likely to be used

        when (arg) {
            "channels" -> {
                PanelAPI.updateChannels(context.guild)
                context.send().success("Updated server channels!").queue()
            }
            "all"->{
                context.guild.sync()
                context.send().success("Syncing guild").queue()
            }
            else -> {
                context.send().error("Not a valid item to refresh!")
            }
        }
    }
}