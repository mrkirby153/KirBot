package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandExecutor

@Command(name = "kotlin")
class TestKotlinCommand : CommandExecutor() {
    override fun execute() {
        Bot.LOG.info("This is a kotlin command!")
    }
}