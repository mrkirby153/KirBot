package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.success
import net.dv8tion.jda.core.entities.Message

@Command(name = "refresh", clearance = Clearance.BOT_OWNER, category = "Admin")
class CommandRefresh : CommandExecutor() {

    override fun execute(message: Message, args: Array<String>) {
        if (args.isEmpty()) {
            message.send().error("Please specify an item to refresh!").queue()
            return
        }

        val arg = args[0]

        when (arg) {
            "channels" -> {
                Database.updateChannels(guild)
                message.send().success("Updated server channels!").queue()
            }
            else -> {
                message.send().error("Not a valid item to refresh!")
            }
        }
    }
}