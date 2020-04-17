package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.tree.CommandNode
import java.io.File

object CommandDocumentationGenerator {

    fun generate(output: File) {
        val commands = Bot.applicationContext.get(CommandExecutor::class.java).getAllLeaves()

        val builder = StringBuilder()
        builder.appendln("# Command List")
        val byCategory = mutableMapOf<CommandCategory, MutableList<CommandNode>>()
        commands.forEach {
            byCategory.getOrPut(it.metadata!!.category) { mutableListOf() }.add(it)
        }
        byCategory.forEach { category, cmds ->
            if(cmds.none { it.metadata?.admin == false }) {
                return@forEach
            }
            builder.appendln("## ${category.friendlyName}")
            builder.append(getHeader())
            for(c in cmds) {
                if(c.metadata?.admin == true)
                    continue
                val h = c.method?.getAnnotation(
                        CommandDescription::class.java)?.value ?: "No description provided"
                builder.appendln("| `${c.parentString} ${c.name} ${c.metadata!!.arguments.joinToString(" ")}` | ${c.metadata!!.clearance} | $h |")
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