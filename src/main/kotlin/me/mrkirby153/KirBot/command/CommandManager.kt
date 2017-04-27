package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.command.executors.ShutdownCommand
import me.mrkirby153.KirBot.command.executors.UpdateNicknames
import me.mrkirby153.KirBot.command.executors.moderation.CommandKick
import me.mrkirby153.KirBot.command.executors.moderation.CommandMute
import me.mrkirby153.KirBot.command.executors.moderation.CommandUnmute
import me.mrkirby153.KirBot.command.executors.music.*
import me.mrkirby153.KirBot.command.executors.polls.CommandPoll
import me.mrkirby153.KirBot.command.executors.search.CommandGoogle
import me.mrkirby153.KirBot.command.executors.server.CommandClean
import me.mrkirby153.KirBot.database.CommandType
import me.mrkirby153.KirBot.database.DBCommand
import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.server.ServerRepository
import me.mrkirby153.KirBot.utils.Cache
import me.mrkirby153.KirBot.utils.getClearance
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * Command handler
 */
object CommandManager {

    val commands = mutableMapOf<String, CommandExecutor>()

    val commandPrefixCache = Cache<String, String>(1000 * 60)

    init {
        register(ShutdownCommand::class)

        // Server management commands
        register(CommandClean::class)

        register(UpdateNicknames::class)
        register(CommandPoll::class)
        register(CommandGoogle::class)

        // Moderation commands
        register(CommandKick::class)
        register(CommandMute::class)
        register(CommandUnmute::class)

        // Music commands
        register(CommandPlay::class)
        register(CommandQueue::class)
        register(CommandSkip::class)
        register(CommandStop::class)
        register(CommandPause::class)
        register(CommandVolume::class)
        register(CommandToggleAdminMode::class)
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
        cmd.permissions = annotation.requiredPermissions
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

        var commandPrefix = this.commandPrefixCache[server.id]

        if (commandPrefix == null) {
            commandPrefix = Database.getCommandPrefix(server)
            this.commandPrefixCache.put(server.id, commandPrefix)
        }

        if (!message.startsWith(commandPrefix))
            return

        message = message.substring(1)
        val parts: Array<String> = message.split(" ").toTypedArray()

        val command = parts[0]

        val args = if (parts.isNotEmpty()) parts.drop(1).toTypedArray() else arrayOf<String>()

        val executor = CommandManager.commands[command.toLowerCase()]


        if (executor == null) {
            val customCommand = Database.getCustomCommand(command.toLowerCase(), server) ?: return
            if (customCommand.clearance.value > author.getClearance(server).value) {
                event.message.send().error("You do not have permission to perform that command").queue({
                    m ->
                    m.delete().queueAfter(10, TimeUnit.SECONDS)
                })
                return
            }
            callCustomCommand(event.channel, customCommand, args, author)
            return
        }

        executor.server = server

        // Verify permissions
        val missingPermissions = arrayListOf<Permission>()
        executor.permissions.forEach { permission ->
            if (!server.getMember(Bot.jda.selfUser).hasPermission(event.channel as Channel, permission)) {
                missingPermissions.add(permission)
            }
        }

        if (missingPermissions.isNotEmpty()) {
            event.message.send().error("I cannot perform that action because I'm missing the following permissions: \n"
                    + missingPermissions.joinToString { " i ${it.name} \n" }).queue()
            return
        }

        if (event.author.getClearance(event.guild).value < executor.clearance.value) {
            event.message.send().error("You do not have permission to perform this command!").queue({
                m ->
                m.delete().queueAfter(10, TimeUnit.SECONDS)
            })
            return
        }
        executor.execute(event.message, args)
    }

    private fun callCustomCommand(channel: MessageChannel, command: DBCommand, args: Array<String>, sender: User) {
        if (command.type == CommandType.TEXT) {
            var response = command.data
            for (i in 0..args.size - 1) {
                response = response.replace("%${i + 1}", args[i])
            }
            channel.sendMessage(response).queue()
        }
    }

    /*private fun executeJavascript(note: Note, script: String, channel: MessageChannel, sender: User, args: Array<String>) {
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
            if (output.length > 2048) {
                note.error("Output is too long")
            } else {
                note.replyEmbed(null, output)
            }
        } catch (e: ScriptException) {
            note.error("An error occurred when executing the script: ```" + e.message + "```")
        }
    }*/

    private data class EcmaVariable(val name: String, val value: Any)
}