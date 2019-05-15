package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.CommandDocumentationGenerator
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.kcutils.child
import net.dv8tion.jda.core.Permission


class CommandHelp {

    @Command(name = "help", arguments = ["[command:string...]"],
            permissions = [Permission.MESSAGE_EMBED_LINKS])
    @CommandDescription("Display help for a command")
    fun execute(context: Context, cmdContext: CommandContext) {
        if (!cmdContext.has("command")) {
            displayAllCommands(context)
        } else {
            displayHelpForCommand(context,
                    cmdContext.get<String>("command")?.split(" ")?.toTypedArray()
                            ?: emptyArray())
        }
    }

    @Command(name = "regen-docs")
    @AdminCommand
    fun regenerateDocs(context: Context, cmdContext: CommandContext) {
        CommandDocumentationGenerator.generate(Bot.files.data.child("commands.md"))
        context.success()
    }

    private fun displayAllCommands(context: Context) {
//        val cmdPrefix = SettingsRepository.get(context.guild, "cmd_prefix", "!")!!
//        val help = CommandExecutor.helpManager.getDedupedHelp()
//        val categorized = mutableMapOf<CommandCategory, List<HelpNode>>()
//        CommandCategory.values().forEach { cat ->
//            categorized[cat] = help.filter { it.category == cat }
//        }
//        var msg = ""
//        CommandCategory.values().forEach {
//            if (checkAppend(msg, "\n__${it.friendlyName}__\n")) {
//                msg += "\n__${it.friendlyName}__\n"
//            } else {
//                context.channel.sendMessage(embed {
//                    description { +msg }
//                    color = Color.BLUE
//                }.build()).queue()
//                msg = ""
//            }
//            categorized[it]?.forEach { help ->
//                var line = "    "
//                line += "**$cmdPrefix${help.command}**"
//                line += " - ${help.help}\n"
//                if (checkAppend(msg, line)) {
//                    msg += line
//                } else {
//                    context.channel.sendMessage(embed {
//                        description { +msg }
//                        color = Color.BLUE
//                    }.build()).queue()
//                    msg = ""
//                }
//            }
//        }
//        val helpInfo = "\nUse `${cmdPrefix}help <command>` for more information about a command"
//        if (!checkAppend(msg, helpInfo)) {
//            context.channel.sendMessage(embed {
//                description { +msg }
//                color = Color.BLUE
//            }.build()).queue()
//            msg = ""
//        } else {
//            msg += helpInfo
//        }
//        context.channel.sendMessage(embed {
//            description { +msg }
//            color = Color.BLUE
//        }.build()).queue()
    }

    private fun checkAppend(msg: String, text: String): Boolean {
        return msg.length + text.length < 2048
    }

    private fun displayHelpForCommand(context: Context, args: Array<String>) {
//        val cmdPrefix = SettingsRepository.get(context.guild, "cmd_prefix", "!")!!
//        var index = 0
//        var cmd = CommandExecutor.helpManager.getHelp(args[index++])
//        while (index < args.size) {
//            if (cmd == null)
//                break
//            cmd = cmd.getChild(args[index++])
//        }
//        if (cmd == null)
//            throw CommandException("No command was found")
//        val cmdString = args.joinToString(" ")
//        context.channel.sendMessage(embed {
//            description {
//                +"__$cmdPrefix${cmdString}__\n"
//                appendln("\n${cmd.help}")
//                appendln("\nUsage: ")
//                appendln("    $cmdPrefix$cmdString ${cmd.args}")
//                if (cmd.children.size > 0) {
//                    appendln("\nSub-Commands: ")
//                    cmd.children.forEach {
//                        appendln("    **$cmdPrefix$cmdString ${it.command}** - ${it.help}")
//                    }
//                }
//            }
//            color = Color.BLUE
//        }.build()).queue()
    }
}