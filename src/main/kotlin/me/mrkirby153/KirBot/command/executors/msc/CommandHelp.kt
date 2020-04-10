package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandDocumentationGenerator
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.CommandExecutor
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.settings.GuildSettings
import me.mrkirby153.kcutils.child
import net.dv8tion.jda.api.Permission
import java.awt.Color
import java.util.LinkedList


class CommandHelp {

    @Command(name = "help", arguments = ["[command:string...]"],
            permissions = [Permission.MESSAGE_EMBED_LINKS],
            category = CommandCategory.MISCELLANEOUS)
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

    @Command(name = "regen-docs", parent = "help")
    @AdminCommand
    fun regenerateDocs(context: Context, cmdContext: CommandContext) {
        CommandDocumentationGenerator.generate(Bot.files.data.child("commands.md"))
        context.success()
    }

    private fun displayAllCommands(context: Context) {
        val cmdPrefix = GuildSettings.commandPrefix.get(context.guild)
        val root = CommandExecutor.getRoot()
        val categorized = root.getChildren().filter { it.metadata != null && it.metadata?.admin == false }.groupBy { it.metadata!!.category }
        var msg = ""
        fun sendHelp() {
            context.channel.sendMessage(embed {
                description { +msg }
                color = Color.BLUE
            }.build()).queue()
            msg = ""
        }
        categorized.forEach { category, commands ->
            if (checkAppend(msg, "\n__${category.friendlyName}__\n")) {
                msg += "\n__${category.friendlyName}__\n"
            } else {
                sendHelp()
                msg += "\n__${category.friendlyName}__\n"
            }
            commands.forEach { help ->
                var line = "    "
                line += "**$cmdPrefix${help.parentString} ${help.name}**"
                line += " - ${help.metadata!!.description ?: "No help provided"}\n"
                if (checkAppend(msg, line)) {
                    msg += line
                } else {
                    sendHelp()
                    msg += line
                }
            }
        }
        val helpInfo = "\nUse `${cmdPrefix}help <command>` for more information about a command"
        if (!checkAppend(msg, helpInfo)) {
            sendHelp()
            msg += helpInfo
        } else {
            msg += helpInfo
        }
        sendHelp()
    }

    private fun checkAppend(msg: String, text: String): Boolean {
        return msg.length + text.length < 2048
    }

    private fun displayHelpForCommand(context: Context, args: Array<String>) {
        val cmdPrefix = GuildSettings.commandPrefix.get(context.guild)
        val node = CommandExecutor.resolve(LinkedList(args.toList())) ?: throw CommandException(
                "No command was found")
        val cmdString = "$cmdPrefix${node.parentString.trim()} ${node.name}"
        context.channel.sendMessage(embed {
            description {
                +"__${cmdString}__\n"
                if(!node.isSkeleton()) {
                    appendln("\n${node.metadata?.description}")
                    appendln("\nUsage: ")
                    appendln("    $cmdString ${node.metadata?.arguments?.joinToString(" ")}")
                }
                if (node.getChildren().isNotEmpty()) {
                    appendln("\nSub-Commands: ")
                    node.getChildren().flatMap { it.getLeaves() }.filter { !it.isSkeleton() }.forEach { child ->
                        appendln(
                                "    **$cmdPrefix${child.parentString} ${child.name}** - ${child.metadata?.description}")
                    }
                }
            }
            color = Color.BLUE
        }.build()).queue()
    }
}