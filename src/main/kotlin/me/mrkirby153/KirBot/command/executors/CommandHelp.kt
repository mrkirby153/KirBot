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
                    field(category.friendlyName, true) {
                        buildString {
                            commands.forEach {
                                appendln("[" + prefix + it.command + "]()")
                            }
                        }
                    }
                }
                appendDescription(buildString {
                    append("\n\nFor custom commands available on this server, ")
                    appendln("Click Here" link "https://kirbot.mrkirby153.tk/${context.guild.id}/commands")
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
                if(spec.examples.isNotEmpty()){
                    field((if(spec.examples.size == 1) "Example" else "Examples"), false, buildString{
                        append("```")
                        spec.examples.forEach {
                            appendln(" + $prefix$it")
                        }
                        append("```")
                    })
                }
            }.rest().queue()
        }
    }
}