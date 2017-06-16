package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getClearance

abstract class MusicCommand : CommandExecutor() {

    abstract fun exec(context: Context, args: Array<String>)

    override fun execute(context: Context, args: Array<String>) {
        if (serverData.musicManager.adminOnly) {
            if (guild.getMember(context.author).getClearance(guild).value < Clearance.SERVER_ADMINISTRATOR.value) {
                context.send().error("The DJ is in Admin-Only mode :cry: \nOnly those with the `ADMINISTRATOR` role can control the music").queue()
                return
            }
        }
        if (!serverData.getMusicData().enabled)
            return
        exec(context, args)
    }
}