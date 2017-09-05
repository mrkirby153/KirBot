package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.Time
import net.dv8tion.jda.core.entities.User
import java.text.SimpleDateFormat

class CommandSeen: CmdExecutor() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: return

        val data = Bot.seenStore.get(user)
        if(data == null){
            context.send().embed("Seen") {
                setDescription("No data recorded")
            }.rest().queue()
        } else {
            context.send().embed("Seen") {
                field("Last Message", false, buildString {
                    if(data.lastMessage != -1L) {
                        append(SimpleDateFormat(Time.DATE_FORMAT_NOW).format(data.lastMessage))

                        append(" (${Time.format(1, System.currentTimeMillis() - data.lastMessage, Time.TimeUnit.FIT)} ago)")
                    } else {
                        append("Unknown")
                    }
                })
                field("Server", false, data.server)
                field("Online", false, data.status)
            }.rest().queue()
        }
    }
}