package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.command.executors.ShutdownCommand
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.server.ServerRepository
import me.mrkirby153.KirBot.utils.Note
import me.mrkirby153.KirBot.utils.getClearance
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import kotlin.reflect.KClass

/**
 * Command handler
 */
object CommandManager {

    val commands = mutableMapOf<String, CommandExecutor>()

    init {
        register(ShutdownCommand::class)
    }

    /**
     * Register a command
     */
    fun register(cls: Class<out CommandExecutor>) {
        if (!cls.isAnnotationPresent(Command::class.java)) {
            throw IllegalArgumentException("@Command annotation missing for ${cls.name}")
        }
        val cmd = cls.newInstance()

        val annotation = cls.getAnnotation(Command::class.java)

        cmd.aliases = annotation.aliases
        cmd.clearance = annotation.clearance
        cmd.description = annotation.description
        commands[annotation.name.toLowerCase()] = cmd

        annotation.aliases.forEach { a -> commands[a.toLowerCase()] = cmd }

        Bot.LOG.info("Registered command ${annotation.name} (${cls.name}")
    }

    fun register(cls: KClass<out CommandExecutor>) {
        register(cls.java)
    }

    fun call(event: MessageReceivedEvent) {
        var message = event.message.rawContent

        val author = event.author ?: return

        if (!message.startsWith("!"))
            return
        message = message.substring(1)
        val parts: Array<String> = message.split(" ").toTypedArray()

        val command = parts[0]

        val args = if (parts.isNotEmpty()) parts.drop(1).toTypedArray() else arrayOf<String>()

        val executor = CommandManager.commands[command.toLowerCase()] ?: return

        if (event.author.getClearance(event.guild).value < executor.clearance.value) {
            event.channel.sendMessage("Error: You do not have permission to perform this command!").queue()
            return
        }

        val server: Server = ServerRepository.getServer(event.guild) ?: return
        executor.execute(Note(server, event.message), server, author, event.channel, args)
    }
}