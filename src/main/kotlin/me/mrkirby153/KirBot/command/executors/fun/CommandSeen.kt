package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.entities.User
import java.text.SimpleDateFormat

@Command("seen")
class CommandSeen : BaseCommand(CommandCategory.FUN, Arguments.user("user")) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: return

        val data = Bot.seenStore.get(user)
        if(data == null){
            context.send().embed("Seen") {
                description { +"No data recorded" }
            }.rest().queue()
        } else {
            context.send().embed("Seen") {
                thumbnail = user.effectiveAvatarUrl
                fields {
                    field {
                        title = "Last Message"
                        inline = false
                        description {
                            if(data.lastMessage != -1L){
                                +SimpleDateFormat(Time.DATE_FORMAT_NOW).format(data.lastMessage)
                                +" (${Time.format(1, System.currentTimeMillis() - data.lastMessage, Time.TimeUnit.FIT)} ago)"
                            }
                        }
                    }
                    field {
                        title = "Server"
                        inline = false
                        description  = data.server
                    }
                    field {
                        title = "Status"
                        description = data.status.toString()
                        inline = false
                    }
                }
            }.rest().queue()
        }
    }
}