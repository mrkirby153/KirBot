package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.CommandManager
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.botUrl
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.embed.u
import java.awt.Color

class CommandHelp : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val prefix = context.shard.serverSettings[context.guild.id]?.cmdDiscriminator ?: "!"
        val command = cmdContext.string("command")
        if (command == null) {
            displayAllHelp(context)
        } else {
            val spec = CommandManager.findCommand(command) ?: throw CommandException("Unknown command!")
            val cmdHelp = CommandManager.helpManager.get(spec.command) ?: throw CommandException("No help exists for this command")
            context.send().embed {
                title { +"Help: ${spec.command.capitalize()}" }
                color = Color.BLUE
                description {
                    appendln(u("Command Name"))
                    appendln("  [$prefix${spec.command}]()")
                    +"\n"
                    appendln(u("Description"))
                    appendln("  ${cmdHelp.description}")
                    +"\n"
                    appendln(u("Usage"))
                    val params = buildString {
                        spec.arguments.forEach {
                            if(it.required)
                                append("<${it.key}>")
                            else
                                append("[${it.key}]")
                            append(" ")
                        }
                    }.trim()
                    appendln("  $prefix${spec.command} $params")
                    if(cmdHelp.params.isNotEmpty()) {
                        +"\n"
                        appendln(u("Parameters"))
                        cmdHelp.params.forEach {
                            +"   - ${it.name}: ${it.description}\n"
                        }
                    }
                    +"\n"
                    appendln(u("Clearance"))
                    val clearance = context.shard.clearanceOverrides[context.guild.id].firstOrNull {
                        it.command.equals(spec.command, true)
                    }
                    appendln(buildString {
                        append(clearance?.clearance?.apply {
                            +"(Overridden) "
                        } ?: spec.clearance)
                    })
                    +"\n"
                    appendln(u("Example Usage"))
                    cmdHelp.usage.forEach {
                        +"  `$prefix$it`\n"
                    }
                    +"\n"
                }
                footer {
                    text {
                        +"Parameters surrounded with < > are required. Parameters surrounded with [ ] are optional"
                    }
                }
            }.rest().queue()
        }
    }

    private fun displayAllHelp(context: Context){
        val prefix = context.shard.serverSettings[context.guild.id]?.cmdDiscriminator ?: "!"
        context.send().embed("Help") {
            color = Color.BLUE
            description {
                +"Below is a list of all the customCommands available. \n Type `${prefix}help <command>` for more info"
                +"\n\nFor custom commands available on this server, "
                +("Click here" link botUrl("${context.guild.id}/customCommands"))
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
                if (context.shard.customCommands[context.guild.id].isNotEmpty())
                    field {
                        title = "Other Commands"
                        inline = true
                        description {
                            context.shard.customCommands[context.guild.id].forEach {
                                +"[$prefix${it.name}]()\n"
                            }
                        }
                    }
            }
        }.rest().queue()
    }
}