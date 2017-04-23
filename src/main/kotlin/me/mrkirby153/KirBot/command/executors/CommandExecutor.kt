package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Note
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User

abstract class CommandExecutor {

    var aliases: Array<String> = arrayOf<String>()

    var description: String = "No descrpition provided"

    var clearance: Clearance = Clearance.USER

    var permissions: Array<Permission> = arrayOf()

    abstract fun execute(note: Note, server: Server, sender: User, channel: MessageChannel, args: Array<String>)

    protected fun getUserByMention(mention: String): User? {
        val id = mention.substring(2, mention.length-1)

        return Bot.jda.getUserById(id)
    }
}