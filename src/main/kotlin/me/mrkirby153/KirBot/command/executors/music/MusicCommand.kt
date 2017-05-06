package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.getClearance
import net.dv8tion.jda.core.entities.Message

abstract class MusicCommand : CommandExecutor() {

    abstract fun exec(message: Message, args: Array<String>)

    override fun execute(message: Message, args: Array<String>) {
        if (serverData.musicManager.adminOnly) {
            if (guild.getMember(message.author).getClearance(guild).value < Clearance.SERVER_ADMINISTRATOR.value) {
                message.send().error("The DJ is in Admin-Only mode :cry: \nOnly those with the `ADMINISTRATOR` role can control the music").queue()
                return
            }
        }
        exec(message, args)
    }
}