package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User

abstract class CommandExecutor {

    var aliases: Array<String> = arrayOf<String>()

    var description: String = "No descrpition provided"

    var clearance: Clearance = Clearance.USER

    var permissions: Array<Permission> = arrayOf()

    lateinit var server: Server

    abstract fun execute(message: Message, args: Array<String>)

    protected fun getUserByMention(mention: String): User? {
        val id = mention.replace("[<@!>]".toRegex(), "")

        return Bot.jda.getUserById(id)
    }
}