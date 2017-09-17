package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.CommandManager
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.link
import java.awt.Color

class CommandHelp : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val prefix = context.shard.serverSettings[context.guild.id]?.cmdDiscriminator ?: "!"
        val command = cmdContext.string("command")
        if (command == null) {
            context.send().embed("Help") {
                color = Color.BLUE
                description {
                    +"Below is a list of all the customCommands available. \n Type `${prefix}help <command>` for more info"
                    +"\n\nFor custom commands available on this server, "
                    +"Click here" link "https://kirbot.mrkirby153.com/${context.guild.id}/customCommands"
                }
                fields {
                    field {
                        title = "Command Prefix"
                        inline = false
                        description = prefix
                    }
                    for ((category, commands) in CommandManager.getCommandsByCategory()) {
                        field {
                            title = category.friendlyName
                            inline = true
                            description {
                                commands.forEach {
                                    +"[$prefix${it.command}]()\n"
                                }
                            }
                        }
                    }
                }
            }.rest().queue()
        } else {
            val spec = CommandManager.findCustomCommand(command) ?: throw CommandException("Unknown command!")
            context.send().embed("Help: ${spec.command.capitalize()}") {
                color = Color.BLUE
                fields {
                    field {
                        title = "Name"
                        description = prefix + spec.command
                    }
                    field {
                        title = "Description"
                        description = spec.description
                    }
                    val c = CommandManager.getEffectivePermission(spec.command, context.guild, spec.clearance)
                    val overridden = c != spec.clearance
                    field {
                        title = "Clearance"
                        description = c.toString().toLowerCase().replace("_", " ").capitalize() + (if (overridden) "*" else "")
                    }
                    if (overridden)
                        field {
                            title = "Original Clearance"
                            description = c.toString().toLowerCase().replace("_", " ").capitalize()
                        }
                    if (spec.aliases.isNotEmpty())
                        field {
                            title = "Aliases"
                            description = spec.aliases.joinToString(", ")
                        }

                    if (spec.permissions.isNotEmpty())
                        field {
                            title = "Required Permissions"
                            description = spec.permissions.joinToString(", ")
                        }

                    field {
                        title = "Usage"
                        description {
                            +"`$prefix"
                            +spec.command
                            +" "
                            spec.arguments.forEach {
                                if (it.required)
                                    +"<${it.key}>"
                                else
                                    +"[${it.key}]"
                                +" "
                            }
                            +"`"
                        }
                    }
                    if (spec.examples.isNotEmpty())
                        field {
                            title = "Example" + (if (spec.examples.size > 1) "s" else "")
                            description {
                                +"```"
                                spec.examples.forEach {
                                    +" + $prefix$it\n"
                                }
                                +"```"
                            }
                        }

                }
            }.rest().queue()
        }
    }
}