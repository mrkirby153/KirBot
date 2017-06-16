package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getClearance

@Command(name = "clearance", description = "Shows your current rank according to the bot")
class CommandClearance : CommandExecutor() {

    override fun execute(context: Context, args: Array<String>) {
        context.send().info("Your rank is: `${context.author.getClearance(guild)}`").queue()
    }
}