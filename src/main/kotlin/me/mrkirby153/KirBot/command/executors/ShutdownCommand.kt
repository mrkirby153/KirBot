package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User

@Command(name = "shutdown", clearance = Clearance.BOT_OWNER)
class ShutdownCommand : CommandExecutor() {
    override fun execute(server: Server, sender: User, channel: MessageChannel, args: Array<String>) {
        channel.sendMessage("Goodbye!")?.queue()
        Bot.stop()
    }
}