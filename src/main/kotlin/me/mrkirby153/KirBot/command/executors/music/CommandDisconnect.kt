package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.modules.music.MusicBaseCommand
import me.mrkirby153.KirBot.modules.music.MusicManager
import me.mrkirby153.KirBot.utils.Context

@Command(name = "disconnect,dc,stop")
class CommandDisconnect : MusicBaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext, manager: MusicManager) {
        manager.disconnect()
        context.channel.sendMessage("Disconnected and cleared the queue").queue()
    }
}