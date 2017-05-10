package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.utils.getClearance
import net.dv8tion.jda.core.entities.Message

@Command(name = "clearance", description = "Shows your current rank according to the bot")
class CommandClearance : CommandExecutor() {

    override fun execute(message: Message, args: Array<String>) {
        message.send().info("Your rank is: `${message.author.getClearance(guild)}`").queue()
    }
}