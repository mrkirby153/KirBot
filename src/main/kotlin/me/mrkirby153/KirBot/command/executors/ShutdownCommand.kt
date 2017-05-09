package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.entities.Message
import java.awt.Color

@Command(name = "shutdown", clearance = Clearance.BOT_OWNER, category = "Admin")
class ShutdownCommand : CommandExecutor() {
    override fun execute(message: Message, args: Array<String>) {
        message.send().embed {
            title = "Shut down"
            description = "Good Bye :wave:"
            color = Color.RED
        }.rest().queue({ Bot.stop() })
    }
}