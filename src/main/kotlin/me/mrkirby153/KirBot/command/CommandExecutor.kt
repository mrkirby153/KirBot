package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.command.args.ArgumentParseException
import me.mrkirby153.KirBot.command.args.ArgumentParser
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.help.HelpManager
import me.mrkirby153.KirBot.database.api.GuildCommand
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.Time
import me.mrkirby153.KirBot.utils.deleteAfter
import me.mrkirby153.KirBot.utils.getClearance
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Guild
import org.reflections.Reflections
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

object CommandExecutor {

    val commands = mutableListOf<CommandSpec>()

    val helpManager = HelpManager()

    fun loadAll() {
        Bot.LOG.info("Starting loading of commands, this may take a while")
        val time = measureTimeMillis {
            val reflections = Reflections("me.mrkirby153.KirBot")

            val commands = reflections.getSubTypesOf(BaseCommand::class.java)

            Bot.LOG.info("Found ${commands.size} commands")

            commands.forEach(CommandExecutor::registerCommand)
        }
        Bot.LOG.info("Commands registered in ${Time.format(1, time, Time.TimeUnit.FIT)}")
        helpManager.load()
    }

    fun execute(context: Context) {
        if (context.channel.type == ChannelType.PRIVATE) {
            return
        }

        val shard = context.shard
        val guild = context.guild

        var message = context.rawContent

        if (message.isEmpty())
            return

        val settings = shard.serverSettings[guild.id] ?: return

        val prefix = settings.cmdDiscriminator

        if (!message.startsWith(prefix)) {
            return
        }

        Bot.LOG.debug("Processing message \"$message\"")

        message = message.substring(prefix.length)

        val parts = message.split(" ").filter { it.isNotEmpty() }.map { it.trim() }.toTypedArray()

        val cmd = parts[0].toLowerCase()

        Bot.LOG.debug("Looking up $cmd on ${guild.id}")

        val args = if (parts.isNotEmpty()) parts.drop(1).toTypedArray() else emptyArray()

        val command = getCommand(cmd)
        if (command == null) {
            executeCustomCommand(context, cmd, args, shard, guild)
            return
        }

        if (!canExecuteInChannel(command, context.channel as Channel)) {
            Bot.LOG.debug("Ignoring command because of channel whitelist")
            return
        }

        if (command.clearance.value > context.author.getClearance(guild).value) {
            Bot.LOG.debug("${context.author.id} was denied access to $cmd due to lack of clearance. Required: ${command.clearance}, Found: ${context.author.getClearance(guild)}")
            context.send().error("You do not have permission to perform this command!").queue {
                it.deleteAfter(10, TimeUnit.SECONDS)
                context.deleteAfter(10, TimeUnit.SECONDS)
            }
            return
        }

        Bot.LOG.debug("Beginning parsing of arguments: [${args.joinToString(",")}]")

        val parser = ArgumentParser(args)

        val cmdContext: CommandContext
        try {
            cmdContext = parser.parse(command.arguments.toTypedArray())
        } catch (e: ArgumentParseException) {
            context.send().error(e.message ?: "Invalid argument format!").queue {
                it.deleteAfter(10, TimeUnit.SECONDS)
                context.deleteAfter(10, TimeUnit.SECONDS)
            }
            return
        }

        Bot.LOG.debug("Parsed: $cmdContext")

        Bot.LOG.debug("Executing command: $cmd")

        try {
            val executor = command.executor
            executor.aliasUsed = cmd
            executor.cmdPrefix = prefix
            executor.execute(context, cmdContext)
        } catch (e: CommandException) {
            context.send().error(e.message ?: "An unknown error has occurred!").queue {
                context.deleteAfter(10, TimeUnit.SECONDS)
                it.deleteAfter(10, TimeUnit.SECONDS)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            context.send().error("An unknown error has occurred, please try again").queue {
                it.deleteAfter(10, TimeUnit.SECONDS)
                context.deleteAfter(10, TimeUnit.SECONDS)
            }
        }
        Bot.LOG.debug("Command execution finished")
    }

    fun executeCustomCommand(context: Context, command: String, args: Array<String>, shard: Shard, guild: Guild) {
        val customCommand = shard.customCommands[guild.id].firstOrNull { it.name.equals(command, true) } ?: return
        if (customCommand.clearance.value > context.author.getClearance(guild).value) {
            context.send().error("You do not have permission to perform that command").queue {
                context.deleteAfter(10, TimeUnit.SECONDS)
                it.deleteAfter(10, TimeUnit.SECONDS)
            }
            return
        }
        if (!canExecuteInChannel(customCommand, context.channel as Channel)){
            return
        }

        var response = customCommand.data
        for(i in 0 until args.size){
            response = response.replace("%${i+1}", args[i])
        }

        context.channel.sendMessage(response).queue()
    }

    fun registerCommand(clazz: Class<*>) {
        Bot.LOG.debug("Registering command ${clazz.canonicalName}")
        try {
            val instance = clazz.newInstance() as? BaseCommand ?: return

            val spec = instance.commandSpec

            val cmdAnnotation = clazz.getAnnotation(Command::class.java)
            spec.aliases.addAll(cmdAnnotation.value.split(","))
            spec.executor = instance

            val clearanceAnnotation = clazz.getAnnotation(RequiresClearance::class.java)
            spec.clearance = clearanceAnnotation?.value ?: Clearance.USER

            commands.add(spec)
        } catch (e: Exception) {
            e.printStackTrace()
            Bot.LOG.error("An error occurred when registering ${clazz.canonicalName}")
        }
    }

    fun getCommandsByCategory() : Map<CommandCategory, List<CommandSpec>> {
        val categories = mutableMapOf<CommandCategory, MutableList<CommandSpec>>()
        this.commands.forEach {
            if(!categories.containsKey(it.category))
                categories[it.category] = mutableListOf()
            categories[it.category]?.add(it)
        }
        return categories
    }

    private fun getCommand(name: String) = this.commands.firstOrNull { it.aliases.map { it.toLowerCase() }.contains(name.toLowerCase()) }

    private fun canExecuteInChannel(command: CommandSpec, channel: Channel): Boolean {
        val data = Bot.getShardForGuild(channel.guild.id)?.serverSettings?.get(channel.guild.id) ?: return false
        return if (command.respectWhitelist) {
            if(data.whitelistedChannels.isEmpty())
                return true
            data.whitelistedChannels.any { it == channel.id }
        } else {
            true
        }
    }

    private fun canExecuteInChannel(command: GuildCommand, channel: Channel): Boolean {
        val data = Bot.getShardForGuild(channel.guild.id)?.serverSettings?.get(channel.guild.id) ?: return false
        return if (command.respectWhitelist) {
            if(data.whitelistedChannels.isEmpty())
               return true
            data.whitelistedChannels.any { it == channel.id }
        } else {
            true
        }
    }

    private fun getEffectivePermission(command: String, guild: Guild, default: Clearance): Clearance {
        return Bot.getShardForGuild(guild.id)!!.clearanceOverrides[guild.id].firstOrNull { it.command.equals(command, true) }?.clearance ?: default
    }
}