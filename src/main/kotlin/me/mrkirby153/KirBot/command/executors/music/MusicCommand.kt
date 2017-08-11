package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.database.api.MusicSettings
import me.mrkirby153.KirBot.music.MusicManager
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getClearance

abstract class MusicCommand : CmdExecutor() {

    abstract fun exec(context: Context, cmdContext: CommandContext, musicData: MusicSettings)

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (context.data.musicManager.adminOnly) {
            if (context.member.getClearance(context.guild).value < Clearance.SERVER_ADMINISTRATOR.value)
                throw CommandException("The DJ is in Admin-Only mode :cry:\n Only those with the `ADMINISTRATOR` role can control the music")
        }
        context.channel.sendTyping().queue()
        val settings = MusicManager.musicSettings[context.guild.id] ?: return
        exec(context, cmdContext, settings)
    }
}