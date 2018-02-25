package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.CommandExecutor
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.botUrl
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.embed.u
import me.mrkirby153.KirBot.utils.mdEscape
import java.awt.Color

@Command(name = "help,?", arguments = ["[command:string...]"])
class CommandHelp : BaseCommand(CommandCategory.MISCELLANEOUS) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!cmdContext.has("command")) {
            displayAllCommands(context)
        } else {
            val command = CommandExecutor.commands.firstOrNull {
                cmdContext.get<String>("command")!!.toLowerCase() in it.aliases
            } ?: throw CommandException("No command was found with that name")
            displayHelpForCommand(context, command)
        }
    }

    private fun displayAllCommands(context: Context) {
        context.send().embed("Help") {
            color = Color.BLUE
            val prefix = cmdPrefix.mdEscape()
            description {
                +"The command prefix for this server is: `${context.kirbotGuild.settings.cmdDiscriminator}` \n\n"
                +"Below is a list of all the commands available. \n Type `$cmdPrefix$aliasUsed <command>` for more info"
                +"\n\nFor a full list of custom commands available on this server, "
                +("Click Here" link (botUrl("${context.guild.id}/customCommands")))
            }
            fields {
                for ((category, commands) in CommandExecutor.getCommandsByCategory()) {
                    val musicEnabled = context.kirbotGuild.musicManager.settings.enabled
                    if (category == CommandCategory.MUSIC && !musicEnabled) {
                        continue
                    }
                    field {
                        title = category.friendlyName
                        inline = true
                        description {
                            commands.forEach {
                                if (it.clearance == Clearance.BOT_OWNER)
                                    return@forEach
                                +("$prefix${it.aliases.first().mdEscape()}" link ".")
                                +"\n"
                            }
                        }
                    }
                }
                val commands = context.kirbotGuild.customCommands
                if (commands.isNotEmpty()) {
                    field {
                        title = "Other Commands"
                        inline = true
                        description {
                            commands.forEach {
                                +("$prefix${it.name.mdEscape()}" link ".")
                                +"\n"
                            }
                        }
                    }
                }
            }
        }.rest().queue()
    }

    private fun displayHelpForCommand(context: Context, command: BaseCommand) {
        val prefix = cmdPrefix.mdEscape()
        val help = CommandExecutor.helpManager.get(command.aliases.first())
        context.send().embed("Help: ${command.aliases.first().mdEscape()}") {
            description {
                appendln(u("Command Name"))
                appendln("  $prefix${command.aliases[0]}")
                +"\n"
                appendln(u("Description"))
                appendln("  ${help?.description ?: "No description provided"}")
                if (command.aliases.size > 1) {
                    appendln("\n" + u("Aliases"))
                    appendln("   - ${command.aliases.drop(1).joinToString(", ").mdEscape()}")
                    +"\n"
                }
                appendln(u("Usage"))
                val params = buildString {
                    command.argumentList.forEach {
                        append(it.replace(Regex("(,[A-Za-z0-9/]*)+"), ""))
                        append(" ")
                    }
                }.trim()
                appendln("  $prefix${command.aliases.first()} $params")
                if (help?.params?.isNotEmpty() == true) {
                    +"\n"
                    appendln(u("Arguments"))
                    help.params.forEach {
                        +"   - ${it.name}: ${it.description}\n"
                    }
                }
                +"\n"
                if (command.subCommands.isNotEmpty()) {
                    appendln(u("Sub-Commands"))
                    command.subCommands.forEach {
                        +"   - $prefix${command.aliases[0]} $it\n"
                    }
                    +"\n"
                }
                appendln(u("Clearance"))
                +command.clearance.friendlyName
                +"\n"
                if (help?.usage?.isNotEmpty() == true) {
                    +"\n"
                    appendln(u("Example Usage"))
                    help.usage.forEach {
                        +"  `$cmdPrefix$it`\n"
                    }
                    +"\n"
                }
            }
            footer {
                text {
                    +"Parameters surrounded with < > are required, Parameters surrounded with [ ] are optional. Surround a string with quotes to treat it as a single argument"
                }
            }
        }.rest().queue()
    }
}