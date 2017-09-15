package me.mrkirby153.KirBot.command

import io.sentry.Sentry
import io.sentry.event.UserBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.command.args.ArgumentParseException
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.args.elements.RestToString
import me.mrkirby153.KirBot.command.executors.CommandHelp
import me.mrkirby153.KirBot.command.executors.UpdateNicknames
import me.mrkirby153.KirBot.command.executors.`fun`.CommandColor
import me.mrkirby153.KirBot.command.executors.`fun`.CommandQuote
import me.mrkirby153.KirBot.command.executors.`fun`.CommandSeen
import me.mrkirby153.KirBot.command.executors.admin.*
import me.mrkirby153.KirBot.command.executors.clearance.CommandOverrideClearance
import me.mrkirby153.KirBot.command.executors.game.CommandOverwatch
import me.mrkirby153.KirBot.command.executors.group.*
import me.mrkirby153.KirBot.command.executors.moderation.*
import me.mrkirby153.KirBot.command.executors.music.*
import me.mrkirby153.KirBot.command.executors.polls.CommandPoll
import me.mrkirby153.KirBot.command.processors.LaTeXProcessor
import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.database.api.GuildCommand
import me.mrkirby153.KirBot.user.Clearance
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

    val cmds = mutableListOf<CommandSpec>()

    init {
        register(CommandSpec("shutdown") {
            clearance = Clearance.BOT_OWNER
            description = "Shuts down the robot"
            executor = CommandShutdown()
            category = CommandCategory.ADMIN
            ignoreWhitelist = true
        })

        register(CommandSpec("clean") {
            clearance = Clearance.BOT_MANAGER
            description = "Deletes the last messages in the channel"
            permissions(Permission.MESSAGE_MANAGE)
            arguments(Arguments.number("amount", min = 0.0, max = 100.0))
            executor = CommandClean()
            category = CommandCategory.MODERATION
            ignoreWhitelist = true
            addExample("clean 10", "clean 5")
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
            ignoreWhitelist = true
            addExample("poll 30m Option 1, Option 2, Option 3", "poll 10m Question;Option1, Option 2, Option 3")
        })

        register(CommandSpec("kick") {
            description = "Kicks a user"
            clearance = Clearance.BOT_MANAGER
            permissions(Permission.KICK_MEMBERS)
            arguments(Arguments.user("user"))
            executor = CommandKick()
            category = CommandCategory.MODERATION
            ignoreWhitelist = true
            addExample("kick @Test Account")
        })

        register(CommandSpec("mute") {
            description = "Mutes a user in the current channel"
            clearance = Clearance.BOT_MANAGER
            permissions(Permission.MANAGE_CHANNEL)
            arguments(Arguments.user("user"))
            executor = CommandMute()
            category = CommandCategory.MODERATION
            ignoreWhitelist = true
            addExample("mute @Test Account")
        })

        register(CommandSpec("unmute") {
            description = "Unmutes a muted user"
            clearance = Clearance.BOT_MANAGER
            permissions(Permission.MANAGE_CHANNEL)
            arguments(Arguments.user("user"))
            executor = CommandUnmute()
            category = CommandCategory.MODERATION
            ignoreWhitelist = true
            addExample("unmute @Test Account")
        })

        register(CommandSpec("hideChannel") {
            description = "Hides the current channel from everyone"
            clearance = Clearance.BOT_MANAGER
            permissions(Permission.MANAGE_CHANNEL)
            executor = CommandHideChannel()
            category = CommandCategory.MODERATION
            ignoreWhitelist = true
        })

        /*register(CommandSpec("play") {
            description = "Plays the following URL or searches youtube for the text"
            arguments(Arguments.rest("data", "URL or Search"))
            executor = CommandPlay()
            category = CommandCategory.MUSIC
            addExample("play Together Forever", "play https://www.youtube.com/watch?v=dQw4w9WgXcQ")
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
            addExample("volume 10", "volume +10")
        })

        register(CommandSpec("adminMode") {
            description = "Toggles admin-only mode of the DJ"
            clearance = Clearance.SERVER_ADMINISTRATOR
            arguments(Arguments.string("action", false))
            executor = CommandToggleAdminMode()
            category = CommandCategory.MUSIC
        })*/

        register(CommandSpec("play") {
            description = "Plays a URL or searches youtube"
            executor = CommandPlay()
            arguments(Arguments.rest("query/url"))
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("playat"){
            description = "Adds a song to a position in the queue"
            executor = CommandPlay()
            arguments(Arguments.number("position", true, 0.0), Arguments.rest("query/url"))
            clearance = Clearance.BOT_MANAGER
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("disconnect") {
            description = "Disconnects the robot from the current voice channel, clearing the queue"
            executor = CommandDisconnect()
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("stop") {
            description = "Pauses the music"
            executor = CommandStop()
            clearance = Clearance.BOT_MANAGER
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("queue") {
            description = "Displays the current music queue"
            executor = CommandQueue()
            arguments(Arguments.string("option", false))
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("skip"){
            description = "Starts a vote to skip the currently playing song"
            arguments(Arguments.string("action", false))
            executor = CommandSkip()
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("dequeue"){
            description = "Removes a song from the queue"
            executor = CommandDeQueue()
            clearance = Clearance.BOT_MANAGER
            arguments(Arguments.number("position"))
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("volume") {
            description = "Changes the robot's volume"
            clearance = Clearance.BOT_MANAGER
            arguments(Arguments.string("volume", false))
            executor = CommandVolume()
            category = CommandCategory.MUSIC
            addExample("volume 10", "volume +10")
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("move"){
            description = "Moves the given song in the queue"
            clearance = Clearance.BOT_MANAGER
            arguments(Arguments.number("song"), Arguments.number("position", false))
            executor = CommandMove()
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("pause"){
            description = "Pauses the music"
            executor = CommandStop()
            clearance = Clearance.BOT_MANAGER
            description = "Pauses the music"
            category = CommandCategory.MUSIC
        })

        register(CommandSpec("connect"){
            aliases = mutableListOf("summon")
            executor = CommandConnect()
            clearance = Clearance.BOT_MANAGER
            description = "Summons KirBot to your current channel"
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
            addExample("help", "help help")
            ignoreWhitelist = true
        })

        register(CommandSpec("history") {
            executor = CommandHistory()
            category = CommandCategory.MISCELLANEOUS
            ignoreWhitelist = true
        })

        register(CommandSpec("overwatch") {
            executor = CommandOverwatch()
            category = CommandCategory.FUN
            arguments(Arguments.regex("battletag", "[A-Za-z0-9]+#[0-9]{4}", true,
                    "Username#0000", "Please enter a valid battle tag (`Username#0000`)"),
                    Arguments.string("region", false, "us, eu, kr"))
            addExample("overwatch Username#0000")
        })

        register(CommandSpec("spamFilter") {
            executor = CommandDisableSpamFilter()
            category = CommandCategory.MODERATION
            ignoreWhitelist = true
            clearance = Clearance.BOT_MANAGER
        })

        register(CommandSpec("color") {
            executor = CommandColor()
            category = CommandCategory.FUN
            arguments(Arguments.string("color", true))
            ignoreWhitelist = false
        })

        register(CommandSpec("permissions") {
            executor = CommandDumpPermissions()
            category = CommandCategory.ADMIN
            arguments(Arguments.string("permission", false))
            ignoreWhitelist = true
        })

        register(CommandSpec("quote") {
            executor = CommandQuote()
            category = CommandCategory.FUN
            arguments(Arguments.number("id"))
            ignoreWhitelist = true
        })

        register(CommandSpec("createGroup") {
            executor = CommandCreateGroup()
            category = CommandCategory.GROUPS
            arguments(Arguments.rest("name", "group"))
            ignoreWhitelist = true
            clearance = Clearance.BOT_MANAGER
        })

        register(CommandSpec("deleteGroup") {
            executor = CommandDeleteGroup()
            category = CommandCategory.GROUPS
            arguments(Arguments.rest("name", "group"))
            ignoreWhitelist = true
            clearance = Clearance.BOT_MANAGER
        })

        register(CommandSpec("leaveGroup") {
            executor = CommandLeaveGroup()
            category = CommandCategory.GROUPS
            arguments(Arguments.rest("name", "group"))
        })

        register(CommandSpec("joinGroup") {
            executor = CommandJoinGroup()
            category = CommandCategory.GROUPS
            arguments(Arguments.rest("name", "group"))
        })

        register(CommandSpec("groups") {
            executor = CommandListGroups()
            category = CommandCategory.GROUPS
        })

        if (Bot.debug)
            register(CommandSpec("su") {
                executor = CommandSu()
                clearance = Clearance.BOT_OWNER
                arguments(Arguments.string("user"), Arguments.rest("command"))
            })

        register(CommandSpec("clearanceOverride") {
            executor = CommandOverrideClearance()
            clearance = Clearance.BOT_MANAGER
            arguments(Arguments.string("command"), Arguments.string("clearance"))
        })

        register(CommandSpec("seen") {
            executor = CommandSeen()
            arguments(Arguments.user("user"))
        })


        /// ------ REGISTER MESSAGE PROCESSORS ------
        registerProcessor(LaTeXProcessor::class)
    }

    fun register(spec: CommandSpec) {
        Bot.LOG.debug("Registering command \"${spec.command}\"")
        cmds.add(spec)
    }

    fun registerProcessor(cls: KClass<out MessageProcessor>) {
        Bot.LOG.debug("Registering processor \"$cls\"")
        messageProcessors.add(cls.java)
    }

    fun execute(context: Context, shard: Shard, guild: Guild) {
        process(context, shard.getServerData(guild), shard)
        if (context.channel.type == ChannelType.PRIVATE)
            return

        var message = context.message.rawContent

        if (message.isEmpty())
            return

        val guildSettings = shard.serverSettings[guild.id] ?: return

        val prefix = guildSettings.cmdDiscriminator

        var mention = false
        if (!message.startsWith(prefix)) {
            if (!context.message.mentionedUsers.contains(context.guild.jda.selfUser))
                return
            else
                mention = true
        }

        // Drop the prefix
        message = if (mention) message.replace(Regex("<@!?[0-9]+>\\s?"), "") else message.substring(prefix.length)

        val parts: Array<String> = message.split(" ").toTypedArray()

        val command = parts[0].toLowerCase()

        Bot.LOG.debug("Attempting lookup for command $command on $guild")

        val args = if (parts.isNotEmpty()) parts.drop(1).toTypedArray() else arrayOf<String>()
        // Command Whitelist

        val whitelistedChannels = guildSettings.whitelistedChannels
        for (i in 0 until this.cmds.size) {
            val c = this.cmds[i]
            if (c.aliases.map { it.toLowerCase() }.contains(command) || c.command.equals(command, true)) {
                // Check channel whitelist
                if (whitelistedChannels.isNotEmpty() && context.channel.id !in whitelistedChannels && !c.ignoreWhitelist) {
                    Bot.LOG.debug("Ignoring command because channel whitelist")
                    return
                }

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

                val clearance = getEffectivePermission(c.command, guild, c.clearance)
                if (clearance.value > context.author.getClearance(context.guild).value) {
                    context.send().error("You do not have permission to perform this command!").queue {
                        it.delete().queueAfter(10, TimeUnit.SECONDS)
                    }
                    return
                }
                // parse arguments
                Bot.LOG.debug("Parsing arguments: [${args.joinToString(",")}]")
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
                Bot.LOG.debug("Executing command \"${c.command}\"")
                try {
                    c.executor.execute(context, cmdContext)
                } catch (e: CommandException) {
                    context.send().error(e.message ?: "An unknown error occurred!").queue()
                } catch (e: Exception) {
                    e.printStackTrace()
                    context.send().error("An unknown error occurred!").queue()
                    Sentry.getContext().apply {
                        user = UserBuilder().apply {
                            setId(context.author.id)
                            setUsername("${context.author.name}#${context.author.discriminator}")
                        }.build()
                        addTag("command", command)
                    }
                    Sentry.capture(e)
                    Sentry.getContext().clear()
                }
                Bot.LOG.debug("Command executed successfully")
                return
            }
        }
        // Call custom customCommands
        Bot.LOG.debug("Checking for custom command \"$command\"")
        val customCommand = findCustomCommand(command, shard.customCommands[guild.id]) ?: return
        val clearance = getEffectivePermission(customCommand.name, guild, customCommand.clearance)
        if (clearance.value > context.author.getClearance(guild).value) {
            context.send().error("You do not have permission to perform that command").queue {
                it.delete().queueAfter(10, TimeUnit.SECONDS)
            }
            return
        }
        // Check channel whitelisting
        if (whitelistedChannels.isNotEmpty() && context.channel.id !in whitelistedChannels && customCommand.respectWhitelist) {
            Bot.LOG.debug("Ignoring command because channel whitelist")
            return
        }
        Bot.LOG.debug("Found command $command, executing")
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

    private fun findCustomCommand(name: String, cmds: List<GuildCommand>): GuildCommand? {
        cmds.forEach {
            if (it.name.equals(name, true))
                return it
        }
        return null
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
            if (proc.matches.isNotEmpty()) {
                Bot.LOG.debug("Found match for processor ${proc.javaClass} [${proc.matches.joinToString(",")}]")
                proc.process(context)
            }
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

    private fun callCustomCommand(channel: MessageChannel, command: GuildCommand, args: Array<String>, sender: User) {
        var response = command.data
        for (i in 0..args.size - 1) {
            response = response.replace("%${i + 1}", args[i])
        }
        channel.sendMessage(response).queue()
    }

    fun findCustomCommand(command: String): CommandSpec? {
        var foundCommand: CommandSpec? = null
        cmds.forEach {
            if (it.command.equals(command, true) || it.aliases.map(String::toLowerCase).contains(command.toLowerCase()))
                foundCommand = it
            return@forEach
        }
        return foundCommand
    }

    fun getEffectivePermission(command: String, guild: Guild, default: Clearance): Clearance {
        val overrides = Bot.getShardForGuild(guild.id)!!.clearanceOverrides[guild.id]
        return overrides
                .firstOrNull { it.command.equals(command, true) }
                ?.clearance
                ?: default
    }
}

class CommandException(message: String) : Exception(message)