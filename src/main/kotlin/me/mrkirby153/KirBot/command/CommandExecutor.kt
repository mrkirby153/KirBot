package me.mrkirby153.KirBot.command

import com.google.common.util.concurrent.ThreadFactoryBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.ArgumentParseException
import me.mrkirby153.KirBot.command.args.ArgumentParser
import me.mrkirby153.KirBot.command.tree.CommandNode
import me.mrkirby153.KirBot.command.tree.CommandNodeMetadata
import me.mrkirby153.KirBot.database.models.CustomCommand
import me.mrkirby153.KirBot.logger.ErrorLogger
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.stats.Statistics
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.SettingsRepository
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.globalAdmin
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.KirBot.utils.toTypedArray
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.ChannelType
import org.json.JSONArray
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import java.lang.reflect.InvocationTargetException
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.system.measureTimeMillis

object CommandExecutor {

    private val rootNode = CommandNode("<<ROOT>>").apply {
        rootNode = true
    }

    private val executorThread = Executors.newFixedThreadPool(2,
            ThreadFactoryBuilder().setDaemon(true).setNameFormat(
                    "KirBot Command Executor-%d").build())
    private val commandWatchdogThread = Executors.newCachedThreadPool(
            ThreadFactoryBuilder().setDaemon(true).setNameFormat("Command Watchdog-%d").build())

    fun loadAll() {
        Bot.LOG.info("Starting loading of commands, this may take a while")
        val time = measureTimeMillis {
            val reflections = Reflections("me.mrkirby153.KirBot", MethodAnnotationsScanner())

            val commands = reflections.getMethodsAnnotatedWith(
                    Command::class.java).map { it.declaringClass }.toSet()

            Bot.LOG.info("Found ${commands.size} classes")

            val instances = commands.map { it.newInstance() }

            instances.forEach(CommandExecutor::register)
        }
        Bot.LOG.info("Commands registered in ${Time.format(1, time, Time.TimeUnit.FIT)}")
    }

    fun register(instance: Any) {
        val methods = instance.javaClass.declaredMethods.filter { method ->
            method.isAnnotationPresent(Command::class.java)
        }

        methods.forEach { method ->
            val commandAnnotation = method.getAnnotation(Command::class.java)
            val log = method.isAnnotationPresent(LogInModlogs::class.java)
            val ignoreWhitelist = method.isAnnotationPresent(IgnoreWhitelist::class.java)
            val admin = method.isAnnotationPresent(AdminCommand::class.java)

            val descriptionAnnotation = method.getAnnotation(CommandDescription::class.java)

            val metadata = CommandNodeMetadata(commandAnnotation.arguments.toList(),
                    commandAnnotation.clearance, commandAnnotation.permissions, admin,
                    ignoreWhitelist, log, descriptionAnnotation?.value, commandAnnotation.category)

            var parentNode = if (commandAnnotation.parent.isNotBlank()) this.rootNode.getChild(
                    commandAnnotation.parent) else this.rootNode
            if (parentNode == null) {
                val node = CommandNode(commandAnnotation.parent)
                this.rootNode.addChild(node)
                parentNode = node
            }

            var p = parentNode
            commandAnnotation.name.split(" ").forEachIndexed { i, cmd ->
                val child = p?.getChild(cmd)
                if (child != null) {
                    if (i == commandAnnotation.name.split(" ").size - 1) {
                        // We're at the end of the chain, insert the node
                        if (child.isSkeleton()) {
                            child.instance = instance
                            child.method = method
                            child.metadata = metadata
                            child.aliases.addAll(commandAnnotation.aliases)
                        } else {
                            throw IllegalArgumentException(
                                    "Attempting to register a command that already exists: ${commandAnnotation.name}")
                        }
                    }
                    p = child
                } else {
                    val node = if (i == commandAnnotation.name.split(" ").size - 1) {
                        CommandNode(cmd, method, instance, metadata).apply {
                            aliases.addAll(commandAnnotation.aliases)
                        }
                    } else {
                        CommandNode(cmd)
                    }
                    p?.addChild(node)
                    p = node
                }
            }
        }
    }

    fun printCommandTree(parent: CommandNode = rootNode, depth: Int = 0) {
        print(" ".repeat(depth * 5))
        println(parent.name)
        parent.getChildren().forEach { child ->
            printCommandTree(child, depth + 1)
        }
    }

    fun execute(context: Context) {
        if (context.channel.type == ChannelType.PRIVATE)
            return // Ignore DMs
        val message = context.contentRaw

        val prefix = SettingsRepository.get(context.guild, "command_prefix", "!", true)!!
        val mention = isMention(message)
        if (!message.startsWith(prefix) && !mention)
            return
        Bot.LOG.debug("Processing command \"$message\"")

        // Strip the command prefix from the message
        val strippedMessage = if (mention) message.replace(Regex("^<#!?\\d{17,18}>\\s?"),
                "") else message.substring(prefix.length)


        val args = LinkedList(strippedMessage.split(" "))
        val rootCommandName = args[0]

        if (args.isEmpty() || strippedMessage.isEmpty()) {
            if (mention) {
                context.channel.sendMessage(
                        "The command prefix on this server is `$prefix`").queue()
                return
            }
        }
        val node = resolve(args)
        if (node == null || node.isSkeleton()) {
            // TODO 5/14/2019 Execute custom command
            val parts = strippedMessage.split(" ")
            executeCustomCommand(context, parts[0], parts.drop(1).toTypedArray())
            return
        }
        val metadata = node.metadata!!

        // TODO 5/14/2019 Parse user defined aliases

        val defaultAlias = context.kirbotGuild.commandAliases.firstOrNull { it.command == "*" }

        if (!canExecuteInChannel(node, context.channel as Channel)) {
            Bot.LOG.debug("Ignoring because whitelist")
            return
        }

        var clearance = defaultAlias?.clearance ?: 0

        if (metadata.clearance > clearance) {
            // The node's clearance is higher than the default, escalate it
            clearance = metadata.clearance
        }

        if (metadata.admin && !context.author.globalAdmin) {
            Bot.LOG.debug("Attempting to execute an admin command while not being a global admin")
            return
        }

        if (clearance > context.author.getClearance(context.guild)) {
            Bot.LOG.debug(
                    "${context.author} was denied access due to lack of clearance. Required $clearance -- Found ${context.author.getClearance(
                            context.guild)}")
            if (SettingsRepository.get(context.guild, "command_silent_fail", "0") == "0") {
                context.channel.sendMessage(
                        ":lock: You do not have permission to perform this command").queue()
            }
            return
        }
        Bot.LOG.debug("Beginning parsing of arguments: [${args.joinToString(", ")}]")
        val parser = ArgumentParser(args.toTypedArray())

        val cmdContext = try {
            parser.parse(metadata.arguments.toTypedArray())
        } catch (e: ArgumentParseException) {
            context.send().error(e.message ?: "An unknown error occurred").queue()
            return
        }

        Bot.LOG.debug("Parsed $cmdContext")

        val missingPermissions = metadata.permissions.filter {
            !context.channel.checkPermissions(it)
        }
        if (missingPermissions.isNotEmpty()) {
            context.send().error(
                    "Missing the required permissions: `${missingPermissions.joinToString(", ")}`")
            return
        }

        Statistics.commandsRan.labels(rootCommandName, context.guild.id.toString()).inc()

        try {
            try {
                val runtime = measureTimeMillis {
                    node.method!!.invoke(node.instance, context, cmdContext)
                }
                Statistics.commandDuration.labels(rootCommandName).observe(runtime.toDouble())
            } catch (e: InvocationTargetException) {
                throw e.targetException
            } finally {
                if (metadata.log) {
                    context.kirbotGuild.logManager.genericLog(LogEvent.ADMIN_COMMAND, ":tools:",
                            "${context.author.logName} Executed `${context.message.contentRaw}` in **#${context.channel.name}**")
                }
            }
        } catch (e: CommandException) {
            context.send().error(e.message ?: "An unknown error has occurred!").queue()
        } catch (e: InterruptedException) {
            // Ignore
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            ErrorLogger.logThrowable(e, context.guild, context.author)
            context.send().error("An unknown error occurred!").queue()
        }
        Bot.LOG.debug("Command execution finished")
    }

    fun executeAsync(context: Context) {
        val future = this.executorThread.submit {
            execute(context)
        }
        commandWatchdogThread.submit {
            try {
                future.get(10, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                Bot.LOG.debug("Command hit timeout, canceling")
                if (!future.cancel(true)) {
                    Bot.LOG.warn("Could not interrupt command: ${context.message.contentRaw}")
                }
                context.send().error("Your command timed out").queue()
            }
        }
    }

    private tailrec fun resolve(arguments: LinkedList<String>,
                                parent: CommandNode = rootNode): CommandNode? {
        if (arguments.peek() == null) {
            if (parent == rootNode)
                return null
            return parent
        }
        val childName = arguments.pop()
        val childNode = parent.getChild(childName)
        if (childNode == null) {
            if (rootNode == parent)
                return null
            return parent
        }
        if (arguments.peek() != null && childNode.getChild(arguments.peek()) == null)
            return childNode
        return resolve(arguments, childNode)
    }

    private fun isMention(message: String): Boolean {
        return message.matches(Regex("^<@!?${Bot.shardManager.getShardById(0).selfUser.id}>.*"))
    }

    fun executeCustomCommand(context: Context, command: String, args: Array<String>) {
        val customCommand = context.kirbotGuild.customCommands.firstOrNull {
            it.name.equals(command, true)
        } ?: return
        if (customCommand.clearance > context.author.getClearance(context.guild)) {
            if (SettingsRepository.get(context.guild, "command_silent_fail", "0") == "0") {
                context.channel.sendMessage(
                        ":lock: You do not have permission to perform this command").queue()
            }
            return
        }
        if (!canExecuteInChannel(customCommand, context.channel as Channel)) {
            return
        }

        var response = customCommand.data
        for (i in 0 until args.size) {
            response = response.replace("%${i + 1}", args[i])
        }

        context.channel.sendMessage(response).queue()
    }

    private fun canExecuteInChannel(command: CommandNode, channel: Channel): Boolean {
        val channels = SettingsRepository.getAsJsonArray(channel.guild, "cmd_whitelist",
                JSONArray())!!.toTypedArray(String::class.java)
        if (command.metadata?.admin == true)
            return true
        return if (command.metadata?.ignoreWhitelist == false) {
            if (channels.isEmpty())
                return true
            channels.any { it == channel.id }
        } else {
            true
        }
    }

    private fun canExecuteInChannel(command: CustomCommand, channel: Channel): Boolean {
        val channels = SettingsRepository.getAsJsonArray(channel.guild, "cmd_whitelist",
                JSONArray())!!.toTypedArray(String::class.java)
        return if (command.respectWhitelist) {
            if (channels.isEmpty())
                return true
            channels.any { it == channel.id }
        } else {
            true
        }
    }

    fun getAllLeaves(): List<CommandNode> {
        return this.rootNode.getLeaves()
    }

}