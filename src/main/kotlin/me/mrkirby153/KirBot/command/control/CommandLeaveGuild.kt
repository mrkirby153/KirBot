package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context


class CommandLeaveGuild {

    @Command(name = "leaveGuild", arguments = ["<guild:string>"])
    @AdminCommand
    fun execute(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<String>("guild")!!

        if (Bot.shardManager.getGuild(id) == null) {
            throw CommandException("I am not a member of this guild")
        }

        Bot.shardManager.getGuild(id)?.leave()?.queue {
            context.success()
        }
    }

}