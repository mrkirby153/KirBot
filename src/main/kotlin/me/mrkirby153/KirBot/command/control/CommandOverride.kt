package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.utils.Context


@Command(name = "override", arguments = ["<state:string>"], admin = true)
class CommandOverride : BaseCommand() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val state = cmdContext.get<String>("state")?.toBoolean() ?: false
        KirBotGuild.setOverride(context.author, state)
        if(state){
            context.send().success("Override mode enabled!", true).queue()
        } else {
            context.send().success("Override mode disabled!", true).queue()
        }
    }
}