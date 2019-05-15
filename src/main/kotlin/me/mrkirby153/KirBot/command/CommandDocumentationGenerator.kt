package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.annotations.Command
import java.io.File

object CommandDocumentationGenerator {

    fun generate(output: File) {
        val commands = CommandExecutor.commands

        val builder = StringBuilder()
        val byCategory = mutableMapOf<CommandCategory, MutableList<BaseCommand>>()
        commands.forEach {
            byCategory.getOrPut(it.category) { mutableListOf() }.add(it)
        }
        byCategory.forEach { category, cmds ->
            builder.appendln("# ${category.friendlyName}")
            builder.append(getHeader())
            for(c in cmds) {
                if(c.javaClass.getAnnotation(Command::class.java)?.admin == true)
                    continue
                val h = CommandExecutor.helpManager.getHelp(c.aliases.first())
                builder.appendln("| `${c.aliases[0]} ${c.argumentList.joinToString(
                        " ")}` | ${c.clearance} | ${h?.help ?: ""} |")
                for (sub in c.subCommands) {
                    val a = sub.split(",").map { it.trim() }
                    val m = c.getSubCommand(a.first()) ?: continue
                    val an = m.getAnnotation(Command::class.java)
                    val sH = h?.getChild(sub)?.help ?: ""
                    builder.appendln(
                            "| `${c.aliases[0]} ${a.joinToString("/")} ${an.arguments.joinToString(
                                    " ")}` | ${an.clearance} | $sH |")
                }
            }
            builder.appendln()
        }
        output.writeText(builder.toString())
        Bot.LOG.info("Command documentation written to ${output.canonicalPath}")
    }

    private fun getHeader(): String {
        return buildString {
            appendln("| Command | Clearance Level | Description |")
            appendln("|---------|-----------------|-------------|")
        }
    }
}