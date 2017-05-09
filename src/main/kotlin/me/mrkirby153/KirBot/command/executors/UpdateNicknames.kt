package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.realname.RealnameHandler
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.success
import net.dv8tion.jda.core.entities.Message

@Command(name = "updatenames", clearance = Clearance.SERVER_ADMINISTRATOR, category = "Nicknames")
class UpdateNicknames : CommandExecutor() {
    override fun execute(message: Message, args: Array<String>) {
        val realnameHandler = RealnameHandler(guild, serverData)
        realnameHandler.updateNames()
        message.send().success("Real names were refreshed from the database!").queue()
    }
}