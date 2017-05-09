package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User

abstract class CommandExecutor {

    lateinit var command: String

    var aliases: Array<String> = arrayOf<String>()

    var description: String = "No descrpition provided"

    var clearance: Clearance = Clearance.USER

    var permissions: Array<Permission> = arrayOf()

    var category: String = "Uncategoriezed"

    lateinit var shard: Shard

    lateinit var serverData: ServerData

    lateinit var guild: Guild

    abstract fun execute(message: Message, args: Array<String>)

    protected fun getUserByMention(mention: String): User? {
        val id = mention.replace("[<@!>]".toRegex(), "")

        return shard.getUserById(id)
    }
}