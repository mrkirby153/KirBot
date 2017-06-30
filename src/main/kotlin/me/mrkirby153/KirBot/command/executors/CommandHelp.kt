package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.CommandManager
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.link
import java.awt.Color

class CommandHelp : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val prefix = CommandManager.commandPrefixCache[context.guild.id] ?: Database.getCommandPrefix(context.guild)
        val command = cmdContext.string("command")
        if (command == null) {
            context.send().embed("Help") {
                setColor(Color.BLUE)
                setDescription("Below is a list of all the commands available. \n Type `${prefix}help <command>` for more info")
                field("Command Prefix", false, prefix)
                for ((category, commands) in CommandManager.getCommandsByCategory()) {
                    field(category, true) {
                        buildString {
                            commands.forEach {
                                appendln("[" + prefix + it.command + "]()")
                            }
                        }
                    }
                }
                appendDescription(buildString {
                    append("\n\nFor custom commands available on this server, ")
                    appendln("Click Here" link "https://kirbot.mrkirby153.tk/commands/${context.guild.id}")
                })
            }.rest().queue()
        } else {
            val spec = CommandManager.findCommand(command) ?: throw CommandException("Unknown command!")
            context.send().embed("Help: ${spec.command.capitalize()}") {
                setColor(Color.BLUE)
                field("Name", false, prefix + spec.command)
                field("Description", false, spec.description)
                field("Clearance", false, spec.clearance.toString().toLowerCase().replace("_", " ").capitalize())
                if (spec.aliases.isNotEmpty())
                    field("Aliases", false, spec.aliases.joinToString(", "))
                if (spec.permissions.isNotEmpty())
                    field("Required Permissions", false, spec.permissions.joinToString(", "))
                field("Usage", false, buildString {
                    append("`$prefix")
                    append(spec.command)
                    append(" ")
                    for (element in spec.arguments) {
                        if (element.required) {
                            append("<${element.key.capitalize()} (${element.friendlyName})>")
                        } else {
                            append("[${element.key.capitalize()} (${element.friendlyName})]")
                        }
                        append(" ")
                    }
                    append("`")
                })
            }.rest().queue()
        }
    }

/*    override fun execute(context: Context, args: Array<String>) {
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
    }*/
}