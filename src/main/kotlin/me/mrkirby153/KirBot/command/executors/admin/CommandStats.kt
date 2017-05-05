package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.localizeTime
import net.dv8tion.jda.core.entities.Message
import java.awt.Color

@Command(name = "stats", clearance = Clearance.USER, description = "Returns some statistics about the robot")
class CommandStats : CommandExecutor() {
    override fun execute(message: Message, args: Array<String>) {
        val jda = Bot.jda

        message.send().embed("Bot Statistics") {
            color = Color.BLUE

            field("Servers", true, jda.guilds.size)
            field("Users", true, jda.users.size)
            val time = System.currentTimeMillis() - Bot.startTime
            field("Uptime", true, localizeTime((time / 1000).toInt()))
        }.rest().queue()
    }
}