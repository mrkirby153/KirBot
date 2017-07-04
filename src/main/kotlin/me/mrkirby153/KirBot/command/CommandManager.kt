package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.command.args.ArgumentParseException
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.args.elements.RestToString
import me.mrkirby153.KirBot.command.executors.CommandHelp
import me.mrkirby153.KirBot.command.executors.UpdateNicknames
import me.mrkirby153.KirBot.command.executors.admin.*
import me.mrkirby153.KirBot.command.executors.game.CommandOverwatch
import me.mrkirby153.KirBot.command.executors.moderation.CommandHideChannel
import me.mrkirby153.KirBot.command.executors.moderation.CommandKick
import me.mrkirby153.KirBot.command.executors.moderation.CommandMute
import me.mrkirby153.KirBot.command.executors.moderation.CommandUnmute
import me.mrkirby153.KirBot.command.executors.music.*
import me.mrkirby153.KirBot.command.executors.polls.CommandPoll
import me.mrkirby153.KirBot.command.processors.LaTeXProcessor
import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.database.CommandType
import me.mrkirby153.KirBot.database.DBCommand
import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Cache
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getClearance
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import java.awt.Color
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import kotlin.reflect.KClass

/**
 * Command handler
 */
object CommandManager {

    val messageProcessors = mutableSetOf<Class<out MessageProcessor>>()

    val commandPrefixCache = Cache<String, String>(1000 * 60)

    val cmds = mutableListOf<CommandSpec>()

    init {
        register(CommandSpec("shutdown") {
            clearance = Clearance.BOT_OWNER
            description = "Shuts down the robot"
            executor = CommandShutdown()
            category = CommandCategory.ADMIN
        })

        register(CommandSpec("clean") {
            clearance = Clearance.BOT_MANAGER
            description = "Deletes the last messages in the channel"
            permissions(Permission.MESSAGE_MANAGE)
            arguments(Arguments.number("amount", min = 0.0, max = 100.0))
            executor = CommandClean()
            category = CommandCategory.MODERATION
        })

        register(CommandSpec("updateNames") {
            description = "Updates nicknames from the database"
            clearance = Clearance.SERVER_ADMINISTRATOR
            executor = UpdateNicknames()
            category = CommandCategory.MISCELLANEOUS
        })

        register(CommandSpec("poll") {
            description = "Creates a poll"
            clearance = Clearance.USER
            permissions(Permission.MESSAGE_ADD_REACTION)
            arguments(Arguments.string("duration"), Arguments.rest("options", "Options"))
            executor = CommandPoll()
            category = CommandCategory.FUN
        })

        register(CommandSpec("kick") {
            description = "Kicks a user"
            clearance = Clearance.BOT_MANAGER
            permissions(Permission.KICK_MEMBERS)
            arguments(Arguments.user("user"))
            executor = CommandKick()
            category = CommandCategory.MODERATION
        })

        register(CommandSpec("mute") {
            description = "Mutes a user in the current channel"
            clearance = Clearance.BOT_MANAGER
            permissions(Permission.MANAGE_CHANNEL)
            arguments(Arguments.user("user"))
            executor = CommandMute()
            category = CommandCategory.MODERATION
        })

        register(CommandSpec("unmute") {
            description = "Unmutes a muted user"
            clearance = Clearance.BOT_MANAGER
            permissions(Permission.MANAGE_CHANNEL)
            arguments(Arguments.user("user"))
            executor = CommandUnmute()
            category = CommandCategory.MODERATION
        })

        register(CommandSpec("hideChannel") {
            description = "Hides the current channel from everyone"
            clearance = Clearance.BOT_MANAGER
            permissions(Permission.MANAGE_CHANNEL)
            executor = CommandHideChannel()
            category = CommandCategory.MODERATION
        })

        register(CommandSpec("play") {
            description = "Plays the following URL or searches youtube for the text"
            arguments(Arguments.rest("data", "URL or Search"))
            executor = CommandPlay()
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("queue") {
            description = "Displays the current play queue"
            arguments(Arguments.string("action", false))
            executor = CommandQueue()
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("skip") {
            description = "Starts a vote to skip the currently playing song"
            arguments(Arguments.string("action", false))
            executor = CommandSkip()
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("stop") {
            description = "Stops and clears the music queue"
            clearance = Clearance.BOT_MANAGER
            executor = CommandStop()
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("pause") {
            description = "Pauses the music"
            clearance = Clearance.BOT_MANAGER
            executor = CommandPause()
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("volume") {
            description = "Changes the robot's volume"
            clearance = Clearance.BOT_MANAGER
            arguments(Arguments.string("volume", false))
            executor = CommandVolume()
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("adminMode") {
            description = "Toggles admin-only mode of the DJ"
            clearance = Clearance.SERVER_ADMINISTRATOR
            arguments(Arguments.string("action", false))
            executor = CommandToggleAdminMode()
            category = CommandCategory.MUSIC
        })


        register(CommandSpec("stats") {
            description = "Displays statistics about the robot"
            clearance = Clearance.USER
            executor = CommandStats()
            category = CommandCategory.MISCELLANEOUS
        })

        register(CommandSpec("clearance") {
            description = "Displays your current clearance level"
            clearance = Clearance.BOT
            executor = CommandClearance()
            category = CommandCategory.ADMIN
        })

        register(CommandSpec("refresh") {
            clearance = Clearance.BOT_OWNER
            arguments(Arguments.string("item"))
            executor = CommandRefresh()
        })

        register(CommandSpec("help") {
            arguments(Arguments.string("command", false))
            executor = CommandHelp()
            category = CommandCategory.MISCELLANEOUS
        })

        register(CommandSpec("history"){
            executor = CommandHistory()
            category = CommandCategory.MISCELLANEOUS
        })

        register(CommandSpec("overwatch"){
            executor = CommandOverwatch()
            category = CommandCategory.FUN
            arguments(Arguments.regex("battletag", "[A-Za-z0-9]+#[0-9]{4}", true,
                    "Username#0000", "Please enter a valid battle tag (`Username#0000`)"),
                    Arguments.string("region", false, "us, eu, kr"))
        })


        /// ------ REGISTER MESSAGE PROCESSORS ------
        registerProcessor(LaTeXProcessor::class)
    }

    fun register(spec: CommandSpec) {
        cmds.add(spec)
    }

    fun registerProcessor(cls: KClass<out MessageProcessor>) {
        messageProcessors.add(cls.java)
    }

    fun execute(context: Context, shard: Shard, guild: Guild) {
        process(context, shard.getServerData(guild), shard)
        if (context.event.isFromType(ChannelType.PRIVATE))
            return

        var message = context.message.rawContent

        if (message.isEmpty())
            return

        var prefix = this.commandPrefixCache[guild.id]

        if (prefix == null) {
            prefix = Database.getCommandPrefix(guild)
            this.commandPrefixCache[guild.id] = prefix
        }

        var mention = false
        if(!message.startsWith(prefix)){
            if(!context.message.mentionedUsers.contains(context.guild.jda.selfUser))
                return
            else
                mention = true
        }

        // Drop the prefix
        message = if(mention) message.replace(Regex("<@!?[0-9]+>\\s?"), "") else message.substring(prefix.length)

        val parts: Array<String> = message.split(" ").toTypedArray()

        val command = parts[0].toLowerCase()

        val args = if (parts.isNotEmpty()) parts.drop(1).toTypedArray() else arrayOf<String>()

        for (i in 0..this.cmds.size - 1) {
            val c = this.cmds[i]
            if (c.aliases.map { it.toLowerCase() }.contains(command) || c.command.equals(command, true)) {
                // Check permissions
                val missingPerms = c.permissions.filter { !context.guild.selfMember.hasPermission(context.channel as TextChannel, it) }
                if (missingPerms.isNotEmpty()) {
                    context.send().embed("Missing Permissions") {
                        setColor(Color.RED)
                        setDescription(buildString {
                            append("I cannot perform this action because I'm missing the following permissions:\n")
                            append("`" + missingPerms.joinToString(",") + "`")
                        })
                    }.rest().queue()
                    return
                }

                if (c.clearance.value > context.author.getClearance(context.guild).value) {
                    context.send().error("You do not have permission to perform this command!").queue {
                        it.delete().queueAfter(10, TimeUnit.SECONDS)
                    }
                    return
                }
                // parse arguments
                val cmdContext = CommandContext()

                var currentArg = 0
                c.arguments.forEach { commandElement ->
                    if (commandElement is RestToString) {
                        cmdContext.put(commandElement.key, commandElement.customParse(args.clone().drop(currentArg)))
                        return@forEach
                    }
                    try {
                        if (currentArg < args.size) {
                            commandElement.parse(args[currentArg++], cmdContext)
                        } else {
                            if (commandElement.required) {
                                throw ArgumentParseException("The argument `${commandElement.key.capitalize()}` is required!")
                            }
                        }
                    } catch (e: ArgumentParseException) {
                        context.send().error(e.message ?: "An unknown error occurred!").queue()
                        return
                    } catch (e: Exception) {
                        context.send().error("An unknown error occurred!").queue()
                        e.printStackTrace()
                        return
                    }
                }
                try {
                    c.executor.execute(context, cmdContext)
                } catch(e: CommandException) {
                    context.send().error(e.message ?: "An unknown error occurred!").queue()
                } catch(e: Exception) {
                    e.printStackTrace()
                    context.send().error("An unknown error occurred!").queue()
                }
                return
            }
        }
        // Call custom commands
        val customCommand = Database.getCustomCommand(command.toLowerCase(), guild) ?: return
        if (customCommand.clearance.value > context.author.getClearance(guild).value) {
            context.send().error("You do not have permission to perform that command").queue {
                it.delete().queueAfter(10, TimeUnit.SECONDS)
            }
            return
        }
        callCustomCommand(context.channel, customCommand, args, context.author)
    }

    fun getCommandsByCategory(): Map<CommandCategory, Array<CommandSpec>> {
        val mutableMap = mutableMapOf<CommandCategory, MutableList<CommandSpec>>()
        cmds.forEach {
            val cmdArray = mutableMap[it.category] ?: mutableListOf<CommandSpec>()
            cmdArray.add(it)
            mutableMap[it.category] = cmdArray
        }

        val toReturn = mutableMapOf<CommandCategory, Array<CommandSpec>>()
        mutableMap.forEach {
            toReturn[it.key] = it.value.toTypedArray()
        }
        return toReturn
    }

    private fun process(context: Context, guildData: ServerData, shard: Shard) {
        var rawMsgText = context.message.content
        // TODO 5/4/2017 Extract to own method
        for (processor in messageProcessors) {
            val proc = processor.newInstance()
            proc.matches = emptyArray()
            proc.guildData = guildData
            proc.shard = shard
            // Compile regex
            val pattern = Pattern.compile("(?<=${escape(proc.startSequence)})(.*?)(?=${escape(proc.endSequence)})")

            val matches = mutableListOf<String>()
            loop@ while (true) {
                val matcher = pattern.matcher(rawMsgText)

                if (matcher.find()) {
                    val part = rawMsgText.substring(matcher.start(), matcher.end())
                    matches.add(part)
                    rawMsgText = rawMsgText.substring(startIndex = Math.min(matcher.end() + proc.endSequence.length, rawMsgText.length))
                    if (rawMsgText.isEmpty())
                        break@loop
                } else {
                    break@loop
                }
            }
            proc.matches = matches.toTypedArray()
            if (proc.matches.isNotEmpty())
                proc.process(context)
            if (proc.stopProcessing)
                break
        }
    }

    private fun escape(string: String): String {
        return buildString {
            string.forEach {
                this@buildString.append("\\$it")
            }
        }
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

    fun findCommand(command: String): CommandSpec? {
        var foundCommand: CommandSpec? = null
        cmds.forEach {
            if (it.command.equals(command, true) || it.aliases.map(String::toLowerCase).contains(command.toLowerCase()))
                foundCommand = it
            return@forEach
        }
        return foundCommand
    }
}

class CommandException(message: String) : Exception(message)