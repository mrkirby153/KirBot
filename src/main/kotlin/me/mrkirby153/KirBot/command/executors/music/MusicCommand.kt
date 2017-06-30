package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getClearance

abstract class MusicCommand : CmdExecutor() {

    abstract fun exec(context: Context, cmdContext: CommandContext)

    override fun execute(context: Context, cmdContext: CommandContext) {
        if(context.data.musicManager.adminOnly){
            if(context.member.getClearance(context.guild).value < Clearance.SERVER_ADMINISTRATOR.value)
            throw CommandException("The DJ is in Admin-Only mode :cry:\n Only those with the `ADMINISTRATOR` role can control the music")
        }
        if(!context.data.getMusicData().enabled)
            return
        exec(context, cmdContext)
    }
}