package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.executors.TestJavaCommand
import me.mrkirby153.KirBot.command.executors.TestKotlinCommand
import kotlin.reflect.KClass

/**
 * Command handler
 */
object CommandManager {

    val commands = mutableMapOf<String, CommandExecutor>()

    init {
        register(TestJavaCommand::class)
        register(TestKotlinCommand::class)
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
}