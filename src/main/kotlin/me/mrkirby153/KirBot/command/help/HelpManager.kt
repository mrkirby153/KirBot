package me.mrkirby153.KirBot.command.help

import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.CommandCategory
import org.reflections.Reflections

class HelpManager {

    private val helpTree = mutableListOf<HelpNode>()


    fun load() {
        helpTree.clear()
        val reflections = Reflections("me.mrkirby153.KirBot")

        val commands = reflections.getTypesAnnotatedWith(
                Command::class.java)

        commands.forEach { clazz ->
            val cmdAnnotation = clazz.getAnnotation(
                    Command::class.java)
            if(cmdAnnotation.admin)
                return@forEach
            val description = clazz.getAnnotation(CommandDescription::class.java)?.value
                    ?: "No description provided"

            val children = clazz.methods.filter { it.getAnnotation(
                    Command::class.java) != null }

            val instance = clazz.newInstance()
            cmdAnnotation.name.split(",").forEach { cmd ->
                val node = HelpNode(clazz.canonicalName, cmd, description,
                        (instance as BaseCommand).category, cmdAnnotation.arguments.joinToString(" "))

                children.forEach { method ->
                    val annotation = method.getAnnotation(
                            Command::class.java)
                    val d = method.getAnnotation(CommandDescription::class.java)?.value
                            ?: "No description provided"
                    val id = "${clazz.canonicalName}${method.toGenericString()}"

                    annotation.name.split(",").forEach { c ->
                        val childNode = HelpNode(id, c, d, CommandCategory.UNCATEGORIZED, annotation.arguments.joinToString(" "))
                        node.addChild(childNode)
                    }
                }
                helpTree.add(node)
            }
        }
    }


    fun getHelp(name: String): HelpNode? {
        return helpTree.firstOrNull { it.command == name }
    }

    fun getDedupedHelp(): List<HelpNode> {
        val help = mutableListOf<HelpNode>()
        val usedIds = mutableListOf<String>()
        helpTree.forEach {
            if (it.id in usedIds) {
                return@forEach
            }
            usedIds.add(it.id)
            help.add(HelpNode(it))
        }
        help.forEach { it.dedupe() }
        return help
    }
}