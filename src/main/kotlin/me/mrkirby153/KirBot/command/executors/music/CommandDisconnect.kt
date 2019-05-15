package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.modules.music.MusicBaseCommand
import me.mrkirby153.KirBot.modules.music.MusicManager
import me.mrkirby153.KirBot.utils.Context

@Command(name = "disconnect,dc,stop")
@CommandDescription("Disconnects the bot from the current voice channel")
class CommandDisconnect : MusicBaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext, manager: MusicManager) {
        if(!isDJ(context.member)){
            if(!alone(context.member)){
                throw CommandException("You must be alone to use this command!")
            }
        }
        manager.disconnect()
        context.channel.sendMessage("Disconnected and cleared the queue").queue()
    }
}