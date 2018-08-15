package me.mrkirby153.KirBot.command

import com.google.common.util.concurrent.ThreadFactoryBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.args.ArgumentParseException
import me.mrkirby153.KirBot.command.args.ArgumentParser
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.help.HelpManager
import me.mrkirby153.KirBot.database.models.CustomCommand
import me.mrkirby153.KirBot.logger.ErrorLogger
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.deleteAfter
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.globalAdmin
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Guild
import org.reflections.Reflections
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.LinkedList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.system.measureTimeMillis

object CommandExecutor {

    val commands = mutableListOf<BaseCommand>()

    val helpManager = HelpManager()

    private val executorThread = Executors.newFixedThreadPool(2,
            ThreadFactoryBuilder().setDaemon(true).setNameFormat(
                    "KirBot Command Executor-%d").build())
    private val commandWatchdogThread = Executors.newCachedThreadPool(ThreadFactoryBuilder().setDaemon(true).setNameFormat("Command Watchdog-%d").build())

    fun loadAll() {
        Bot.LOG.info("Starting loading of commands, this may take a while")
        val time = measureTimeMillis {
            val reflections = Reflections("me.mrkirby153.KirBot")

            val commands = reflections.getSubTypesOf(BaseCommand::class.java)

            Bot.LOG.info("Found ${commands.size} commands")

            commands.forEach(CommandExecutor::registerCommand)
        }
        Bot.LOG.info("Commands registered in ${Time.format(1, time, Time.TimeUnit.FIT)}")
    }

    fun execute(context: Context) {
        val future = this.executorThread.submit({
            if (context.channel.type == ChannelType.PRIVATE)
                return@submit

            var message = context.contentRaw
            if (message.isEmpty())
                return@submit

            val settings = context.kirbotGuild.settings
            val prefix = settings.cmdDiscriminator

            val botId = context.guild.selfMember.user.id

            val isMention = message.matches(Regex("^<@!?$botId>.*"))

            if (!message.startsWith(prefix)) {
                if (!isMention)
                    return@submit
            }
            Bot.LOG.debug("Processing message \"$message\"")

            message = if (isMention) message.replace(Regex("^<@!?$botId>\\s?"),
                    "") else message.substring(prefix.length)
            val parts = message.split(" ").toTypedArray()
            if ((parts.isEmpty() || message.isEmpty()) && isMention) {
                context.channel.sendMessage(
                        "The command prefix on this server is `$prefix`").queue()
                return@submit
            }
            val cmd = parts[0].toLowerCase()
            Bot.LOG.debug("Looking up $cmd on ${context.guild.id}")

            val a = if (parts.isNotEmpty()) parts.drop(1).toTypedArray() else emptyArray()
            val args = LinkedList<String>()
            args.addAll(a)

            val command = getCommand(cmd) ?: getCommandByAlias(cmd, context.guild)

            val alias = context.kirbotGuild.commandAliases.firstOrNull { it.command == cmd }

            if (command == null) {
                executeCustomCommand(context, cmd, args.toTypedArray())
                return@submit
            }

            if (!canExecuteInChannel(command, context.channel as Channel)) {
                Bot.LOG.debug("Ignoring because whitelist")
                return@submit
            }

            val cmdMethod: Method
            val annotation: Command
            val logInModLogs: Boolean
            if (args.isNotEmpty() && command.hasSubCommand(args[0])) {
                cmdMethod = command.getSubCommand(args[0])!!
                annotation = cmdMethod.getAnnotation(Command::class.java)
                logInModLogs = cmdMethod.getAnnotation(LogInModlogs::class.java) != null
                args.pop()
            } else {
                cmdMethod = command.javaClass.getMethod("execute", Context::class.java,
                        CommandContext::class.java)
                annotation = command.javaClass.getAnnotation(Command::class.java)
                logInModLogs = command.javaClass.getAnnotation(LogInModlogs::class.java) != null
            }

            if (cmdMethod == null)
                return@submit

            val clearance = if (alias != null && alias.clearance != -1) alias.clearance else annotation?.clearance
                    ?: 0
            if(annotation.admin && !context.author.globalAdmin)
                return@submit
            if (clearance > context.author.getClearance(context.guild)) {
                Bot.LOG.debug(
                        "${context.author.id} was denied access to $cmd due to lack of clearance. Required $clearance -- Found: ${context.author.getClearance(
                                context.guild)}")
                context.channel.sendMessage(
                        ":lock: You do not have permission to perform this command").queue()
                return@submit
            }

            Bot.LOG.debug("Beginning parsing of arguments: [${args.joinToString(", ")}")
            val parser = ArgumentParser(args.toTypedArray())

            val cmdContext = try {
                parser.parse(annotation.arguments)
            } catch (e: ArgumentParseException) {
                context.send().error(e.message ?: "An unknown error occurred").queue()
                return@submit
            }

            Bot.LOG.debug("Parsed: $cmdContext")
            Bot.LOG.debug("Executing command: $cmd")

            val missing = annotation.permissions.filter { !context.channel.checkPermissions(it) }
            if (missing.isNotEmpty()) {
                context.send().error(
                        "Missing the required permissions `${missing.joinToString(", ")}`").queue()
                return@submit
            }

            try {
                command.aliasUsed = cmd
                command.cmdPrefix = prefix
                try {
                    if (logInModLogs)
                        context.kirbotGuild.logManager.genericLog(LogEvent.ADMIN_COMMAND, ":tools:",
                                "${context.author.logName} Executed `${context.message.contentRaw}` in **#${context.channel.name}**")
                    cmdMethod.invoke(command, context, cmdContext)
                } catch (e: InvocationTargetException) {
                    throw e.targetException
                }
            } catch (e: CommandException) {
                context.send().error(e.message ?: "An unknown error has occurred!").queue()
            } catch(e: InterruptedException){
                Bot.LOG.debug("Caught interrupted exception from command. Ignoring")
            } catch (e: Exception) {
                e.printStackTrace()
                ErrorLogger.logThrowable(e, context.guild, context.author)
                context.send().error("An unknown error occurred!").queue()
            }
            Bot.LOG.debug("Command execution finished")
        })
        commandWatchdogThread.submit {
            try {
                future.get(10, TimeUnit.SECONDS)
            } catch (e: TimeoutException) {
                Bot.LOG.debug("Command hit timeout, canceling")
                future.cancel(true)
                context.send().error("An unknown error has occurred!").queue()
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    fun executeCustomCommand(context: Context, command: String, args: Array<String>) {
        val customCommand = context.kirbotGuild.customCommands.firstOrNull {
            it.name.equals(command, true)
        } ?: return
        if (customCommand.clearance > context.author.getClearance(context.guild)) {
            context.send().error("You do not have permission to perform that command").queue {
                context.deleteAfter(10, TimeUnit.SECONDS)
                it.deleteAfter(10, TimeUnit.SECONDS)
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

    fun registerCommand(clazz: Class<*>) {
        Bot.LOG.debug("Registering command ${clazz.canonicalName}")
        try {
            val instance = clazz.newInstance() as? BaseCommand ?: return
            commands.add(instance)
        } catch (e: Exception) {
            ErrorLogger.logThrowable(e)
            Bot.LOG.error("An error occurred when registering ${clazz.canonicalName}")
        }
    }

    fun getCommandsByCategory(): Map<CommandCategory, List<BaseCommand>> {
        val categories = mutableMapOf<CommandCategory, MutableList<BaseCommand>>()
        this.commands.forEach {
            if (!categories.containsKey(it.category))
                categories[it.category] = mutableListOf()
            categories[it.category]?.add(it)
        }
        return categories
    }

    private fun getCommand(name: String) = this.commands.firstOrNull {
        it.aliases.map { it.toLowerCase() }.contains(name.toLowerCase())
    }

    private fun getCommandByAlias(name: String, guild: Guild): BaseCommand? {
        val alias = guild.kirbotGuild.commandAliases.firstOrNull { it.command == name }
                ?: return null
        if (alias.alias != null) {
            return getCommand(alias.alias!!)
        }
        return null
    }

    private fun canExecuteInChannel(command: BaseCommand, channel: Channel): Boolean {
        val data = channel.guild.kirbotGuild.settings
        return if (command.respectWhitelist) {
            if (data.cmdWhitelist.isEmpty())
                return true
            data.cmdWhitelist.any { it == channel.id }
        } else {
            true
        }
    }

    private fun canExecuteInChannel(command: CustomCommand, channel: Channel): Boolean {
        val data = channel.guild.kirbotGuild.settings
        return if (command.respectWhitelist) {
            if (data.cmdWhitelist.isEmpty())
                return true
            data.cmdWhitelist.any { it == channel.id }
        } else {
            true
        }
    }

}