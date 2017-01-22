package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.command.executors.ShutdownCommand
import me.mrkirby153.KirBot.command.executors.customCommand.AddCommand
import me.mrkirby153.KirBot.command.executors.customCommand.DeleteCommand
import me.mrkirby153.KirBot.command.executors.customCommand.ListCommands
import me.mrkirby153.KirBot.command.executors.customCommand.SetClearance
import me.mrkirby153.KirBot.command.executors.server.CommandClean
import me.mrkirby153.KirBot.command.executors.server.SetCommandPrefix
import me.mrkirby153.KirBot.server.ServerRepository
import me.mrkirby153.KirBot.server.data.CommandType
import me.mrkirby153.KirBot.server.data.CustomServerCommand
import me.mrkirby153.KirBot.utils.Note
import me.mrkirby153.KirBot.utils.getClearance
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.io.PrintWriter
import java.io.StringWriter
import javax.script.ScriptContext
import javax.script.ScriptEngineManager
import javax.script.ScriptException
import javax.script.SimpleBindings
import kotlin.reflect.KClass

/**
 * Command handler
 */
object CommandManager {

    val commands = mutableMapOf<String, CommandExecutor>()

    init {
        register(ShutdownCommand::class)

        // Custom command commands
        register(AddCommand::class)
        register(DeleteCommand::class)
        register(SetClearance::class)
        register(ListCommands::class)


        // Server management commands
        register(SetCommandPrefix::class)
        register(CommandClean::class)
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

        val server = ServerRepository.getServer(event.guild) ?: return

        if (!message.startsWith(server.data().commandPrefix))
            return
        message = message.substring(1)
        val parts: Array<String> = message.split(" ").toTypedArray()

        val command = parts[0]

        val args = if (parts.isNotEmpty()) parts.drop(1).toTypedArray() else arrayOf<String>()

        val executor = CommandManager.commands[command.toLowerCase()]


        val note = Note(server, event.message)
        if (executor == null) {
            // Check for custom server command
            val customCommand = server.data().commands[command.toLowerCase()] ?: return
            if (customCommand.clearance.value > author.getClearance(server).value) {
                note.error("You do not have permission to perform this command!")
                note.delete(10)
                return
            }
            callCustomCommand(note, event.channel, customCommand, args, author)
            return
        }

        if (event.author.getClearance(event.guild).value < executor.clearance.value) {
            note.error("You do not have permission to perform this command!")
            note.delete(10)
            return
        }
        executor.execute(note, server, author, event.channel, args)
    }

    private fun callCustomCommand(note: Note, channel: MessageChannel, customServerCommand: CustomServerCommand, args: Array<String>, sender: User) {
        if (customServerCommand.type == CommandType.TEXT)
            note.replyEmbed(null, customServerCommand.command)
        else if (customServerCommand.type == CommandType.JS) {
            executeJavascript(note, customServerCommand.command, channel, sender, args)
        }
    }

    private fun executeJavascript(note: Note, script: String, channel: MessageChannel, sender: User, args: Array<String>) {
        val scriptEngine = ScriptEngineManager().getEngineByName("nashorn")

        val vars = arrayOf(EcmaVariable("channel", channel), EcmaVariable("sender", sender.name), EcmaVariable("args", args))

        val bindings = SimpleBindings()
        for ((name, value) in vars) {
            bindings[name] = value
        }
        scriptEngine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE)

        val sw = StringWriter()
        val pw = PrintWriter(sw)
        scriptEngine.context.writer = pw

        try {
            scriptEngine.eval(script)
            note.info(sw.buffer.toString())
        } catch (e: ScriptException) {
            note.error("An error occurred when executing the script: ```" + e.message + "```")
        }
    }

    private data class EcmaVariable(val name: String, val value: Any)
}