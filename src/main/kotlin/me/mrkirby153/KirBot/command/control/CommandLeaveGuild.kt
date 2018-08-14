package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context

@Command(name = "leaveGuild", arguments = ["<guild:string>"], admin = true)
class CommandLeaveGuild : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<String>("guild")!!

        if (Bot.shardManager.getGuild(id) == null) {
            throw CommandException("I am not a member of this guild")
        }

        Bot.shardManager.getGuild(id)?.leave()?.queue {
            context.success()
        }
    }

}