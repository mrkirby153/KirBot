package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandManager
import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.server.Server
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.link
import java.awt.Color

@Command(name = "help", aliases = arrayOf("h"), description = "Shows this help message")
class CommandHelp : CommandExecutor() {

    override fun execute(message: Message, args: Array<String>) {
        val prefix = CommandManager.commandPrefixCache[message.guild.id] ?: Database.getCommandPrefix(Server(message.guild))

        if (args.isEmpty())
            message.send().embed("Help") {
                color = Color.BLUE
                description = "Below is a list of all the commands available.\n Type `${prefix}help <command>` for more info"
                val executors = mutableListOf<CommandExecutor>()
                for ((name, executor) in CommandManager.commands) {
                    if (executors.contains(executor))
                        continue
                    executors.add(executor)
                    field("", true) {
                        buildString {
                            append("[").append(prefix).append(name).appendln("]()")
                        }
                    }
                }
                appendDescription(buildString {
                    append("\n\nFor custom commands available on this server, ")
                    // TODO 4/30/2017 Replace with correct, working URL
                    appendln("Click Here" link "@ACTUAL_URL@/commands/${server.id}")
                })
            }.rest().queue()
        else {
            val command = args[0]
            val executor = CommandManager.commands[command]
            if (executor == null)
                message.send().embed("Help") {
                    color = Color.RED
                    description = "There is no command by that name!"
                }.rest().queue()
            else
                message.send().embed("Command Information") {
                    color = Color.BLUE
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