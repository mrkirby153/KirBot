package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.realname.RealnameHandler
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context

@Command(name = "updatenames", clearance = Clearance.SERVER_ADMINISTRATOR, category = "Nicknames")
class UpdateNicknames : CommandExecutor() {
    override fun execute(context: Context, args: Array<String>) {
        val realnameHandler = RealnameHandler(guild, serverData)
        realnameHandler.updateNames()
        context.send().success("Real names were refreshed from the database!").queue()
    }
}