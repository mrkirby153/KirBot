package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.command.executors.ShutdownCommand
import me.mrkirby153.KirBot.command.executors.UpdateNicknames
import me.mrkirby153.KirBot.command.executors.server.CommandClean
import me.mrkirby153.KirBot.database.CommandType
import me.mrkirby153.KirBot.database.DBCommand
import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.server.ServerRepository
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

        // Server management commands
        register(CommandClean::class)

        register(UpdateNicknames::class)
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

        // TODO 4/20/2017 Accept custom command prefixes
        if (!message.startsWith("!"))
            return
        message = message.substring(1)
        val parts: Array<String> = message.split(" ").toTypedArray()

        val command = parts[0]

        val args = if (parts.isNotEmpty()) parts.drop(1).toTypedArray() else arrayOf<String>()

        val executor = CommandManager.commands[command.toLowerCase()]


        val note = Note(server, event.message)
        if (executor == null) {
            val customCommand = Database.getCustomCommand(command.toLowerCase(), server) ?: return
            if (customCommand.clearance.value > author.getClearance(server).value) {
                note.error("You do not have permission to perform that command!").get().delete(10)
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

    private fun callCustomCommand(note: Note, channel: MessageChannel, command: DBCommand, args: Array<String>, sender: User) {
        if (command.type == CommandType.TEXT) {
            var response = command.data
            for (i in 0..args.size - 1) {
                response = response.replace("%${i+1}", args[i])
            }
            channel.sendMessage(response).queue()
        } else if (command.type == CommandType.JAVASCRIPT) {
            executeJavascript(note, command.data, channel, sender, args)
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
            val output = sw.buffer.toString()
            if(output.length > 2048){
                note.error("Output is too long")
            } else {
                note.replyEmbed(null, output)
            }
        } catch (e: ScriptException) {
            note.error("An error occurred when executing the script: ```" + e.message + "```")
        }
    }

    private data class EcmaVariable(val name: String, val value: Any)
}