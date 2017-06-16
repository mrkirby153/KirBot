package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import java.awt.Color

@Command(name = "shutdown", clearance = Clearance.BOT_OWNER, category = "Admin")
class ShutdownCommand : CommandExecutor() {
    override fun execute(context: Context, args: Array<String>) {
        context.send().embed("Shut Down") {
            setColor(Color.RED)
            setDescription("Good Bye :wave:")
        }.rest().queue({ Bot.stop() })
    }
}