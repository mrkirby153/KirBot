package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandManager
import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.link
import java.awt.Color

@Command(name = "help", aliases = arrayOf("h"), description = "Shows this help message")
class CommandHelp : CommandExecutor() {

    override fun execute(context: Context, args: Array<String>) {
        val prefix = CommandManager.commandPrefixCache[context.guild.id] ?: Database.getCommandPrefix(context.guild)

        if (args.isEmpty())
            context.send().embed("Help") {
                setColor(Color.BLUE)
                setDescription("Below is a list of all the commands available.\n Type `${prefix}help <command>` for more info")
                field("Command Prefix", false, prefix)
                val usedExecutors = mutableListOf<CommandExecutor>()
                for ((category, commands) in CommandManager.getCommandsByCategory()) {
                    field(category, true) {
                        buildString {
                            commands.forEach {
                                if (it in usedExecutors)
                                    return@forEach
                                usedExecutors.add(it)
                                appendln("[" + prefix + it.command + "]()")
                            }
                        }
                    }
                }
                appendDescription(buildString {
                    append("\n\nFor custom commands available on this server, ")
                    // TODO 4/30/2017 Replace with correct, working URL
                    appendln("Click Here" link "https://kirbot.mrkirby153.tk/commands/${context.guild.id}")
                })
            }.rest().queue()
        else {
            val command = args[0]
            val executor = CommandManager.commands[command]
            if (executor == null)
                context.send().embed("Help") {
                    setColor(Color.BLUE)
                    description = "There is no command by that name!"
                }.rest().queue()
            else
                context.send().embed("Command Information") {
                    setColor(Color.BLUE)
                    field("Name", false, prefix + command)
                    field("Description", false, executor.description)
                    field("Clearance", false, executor.clearance)
                    if (executor.aliases.isNotEmpty())
                        field("Aliases", false, executor.aliases.joinToString(", "))
                    field("Required Permissions", false, executor.permissions.joinToString(", "))
                }.rest().queue()
        }
    }
}