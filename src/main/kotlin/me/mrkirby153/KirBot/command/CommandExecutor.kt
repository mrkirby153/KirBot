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
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.AdminControl
import me.mrkirby153.KirBot.sharding.Shard
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.mdEscape
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Guild
import org.reflections.Reflections
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

object CommandExecutor {

    val commands = mutableListOf<BaseCommand>()

    val helpManager = HelpManager()

    private val executorThread = Executors.newFixedThreadPool(2,
            ThreadFactoryBuilder().setDaemon(true).setNameFormat(
                    "KirBot Command Executor-%d").build())

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
        this.executorThread.submit({
            if (context.channel.type == ChannelType.PRIVATE) {
                return@submit
            }

            val shard = context.shard
            val guild = context.kirbotGuild

            var message = context.contentRaw

            if (message.isEmpty())
                return@submit

            val settings = guild.settings

            val prefix = settings.cmdDiscriminator

            val isMention = message.matches(Regex("^<@!?${context.jda.selfUser.id}>.*"))

            if (!message.startsWith(prefix)) {
                if (!isMention)
                    return@submit
            }

            Bot.LOG.debug("Processing message \"$message\"")

            message = if (isMention) message.replace(Regex("^<@!?${context.jda.selfUser.id}>"),
                    "") else message.substring(prefix.length)

            val parts = message.split(
                    " ").filter { it.isNotEmpty() }.map { it.trim() }.toTypedArray()
            if (parts.isEmpty() && isMention) {
                context.send().info("The command prefix on this server is `$prefix`").queue()
                return@submit
            }
            var cmd = parts[0].toLowerCase()

            Bot.LOG.debug("Looking up $cmd on ${guild.id}")

            val args = if (parts.isNotEmpty()) parts.drop(1).toTypedArray() else emptyArray()

            val aliasedCommand = guild.commandAliases.firstOrNull { it.command == cmd }
            if(aliasedCommand?.alias != null) {
                Bot.LOG.debug("Command has an overridden alias. $cmd -> ${aliasedCommand.alias}")
                cmd = aliasedCommand.alias!!
            }
            val command = getCommand(cmd)
            if (command == null) {
                executeCustomCommand(context, cmd, args, shard, guild)
                return@submit
            }
            // Here we check for sub-command

            // Check the first argument for sub-command
            val isSubCommand = if (args.isNotEmpty()) command.hasSubCommand(args[0]) else false
            val subCommand = if (args.isNotEmpty()) args[0] else ""

            Bot.LOG.debug("Subcommand? $isSubCommand")

            val effectiveArgs = if (isSubCommand) args.drop(1) else args.toList()

            if (!canExecuteInChannel(command, context.channel as Channel)) {
                Bot.LOG.debug("Ignoring command because of channel whitelist")
                return@submit
            }

            val clearance = if(aliasedCommand != null && aliasedCommand.clearance != -1) aliasedCommand.clearance else command.clearance
            Bot.LOG.debug("Effective clearance is $clearance")
            if (!isSubCommand && clearance > context.author.getClearance(guild)) {
                Bot.LOG.debug(
                        "${context.author.id} was denied access to $cmd due to lack of clearance. Required: ${command.clearance}, Found: ${context.author.getClearance(
                                guild)}")
                context.send().error("You do not have permission to perform this command!").queue()
                return@submit
            }

            if (isSubCommand && command.getSubCommandClearance(
                    subCommand) > context.author.getClearance(guild)) {
                context.send().error("You do not have permission to perform this command!").queue()
                return@submit
            }


            Bot.LOG.debug("Beginning parsing of arguments: [${effectiveArgs.joinToString(",")}]")

            val parser = ArgumentParser(effectiveArgs.toTypedArray())

            val cmdContext: CommandContext
            cmdContext = try {
                if (!isSubCommand) {
                    parser.parse(command.argumentList)
                } else {
                    Bot.LOG.debug("Processing sub-command arguments")
                    val sc = command.getSubCommand(subCommand) ?: throw ArgumentParseException(
                            "Error: Sub-command retrieval failed")
                    parser.parse(sc.getAnnotation(Command::class.java).arguments)
                }
            } catch (e: ArgumentParseException) {
                context.send().error(e.message ?: "Invalid argument format!").queue()
                return@submit
            }

            Bot.LOG.debug("Parsed: $cmdContext")

            Bot.LOG.debug("Executing command: $cmd")

            try {
                command.aliasUsed = cmd
                command.cmdPrefix = prefix
                // Log the command in the modlogs
                if (command.javaClass.getAnnotation(
                                LogInModlogs::class.java) != null || (isSubCommand && command.getSubCommand(
                                subCommand)?.getAnnotation(LogInModlogs::class.java) != null))
                    guild.logManager.genericLog(LogEvent.ADMIN_COMMAND, ":tools:",
                            "${context.author.nameAndDiscrim} (`${context.author.id}`) Executed `${context.message.contentRaw}` in **#${context.channel.name.mdEscape()}**")
                if (isSubCommand) {
                    Bot.LOG.debug("Executing sub-command $subCommand")
                    try {
                        command.getSubCommand(subCommand)?.invoke(command, context, cmdContext)
                    } catch (e: InvocationTargetException) {
                        throw e.targetException
                    }
                } else {
                    command.execute(context, cmdContext)
                }
            } catch (e: CommandException) {
                context.send().error(e.message ?: "An unknown error has occurred!").queue()
            } catch (e: Exception) {
                e.printStackTrace()
                val id = ErrorLogger.logThrowable(e, context.guild, context.author)
                context.send().error(
                        "An unknown error has occurred, please try again. \nThis error can be referenced with id: `$id`").queue()
            }
            Bot.LOG.debug("Command execution finished")
        })
    }

    fun executeCustomCommand(context: Context, command: String, args: Array<String>, shard: Shard,
                             guild: Guild) {
        val customCommand = context.kirbotGuild.customCommands.firstOrNull {
            it.name.equals(command, true)
        } ?: return
        if (customCommand.clearance > context.author.getClearance(guild)) {
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

    private fun canExecuteInChannel(command: BaseCommand, channel: Channel): Boolean {
        val data = channel.guild.kirbotGuild.settings
        return if (command.respectWhitelist) {
            if (data.cmdWhitelist.isEmpty())
                return true
            data.cmdWhitelist.any { it == channel.id }
        } else if (command.controlCommand && channel.id != ModuleManager[AdminControl::class.java].logChannel?.id) {
            return false
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