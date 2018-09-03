package me.mrkirby153.KirBot.command.help

import me.mrkirby153.KirBot.command.CommandCategory

open class HelpNode(val id: String, val command: String, val help: String, val category: CommandCategory, val args: String) {

    constructor(copy: HelpNode) : this(copy.id, copy.command, copy.help, copy.category, copy.args) {
        copy.children.forEach {
            val new = HelpNode(it)
            addChild(new)
        }
    }

    val children = mutableListOf<HelpNode>()

    var parent: HelpNode? = null

    fun addChild(node: HelpNode) {
        node.parent = this
        children.add(node)
    }

    fun getChild(subCommand: String): HelpNode? {
        return children.firstOrNull { it.command == subCommand }
    }

    fun dedupe() {
        val usedIds = mutableListOf<String>()
        val toRemove = mutableListOf<HelpNode>()
        children.forEach {
            if (it.id in usedIds) {
                toRemove.add(it)
                return@forEach
            } else {
                usedIds.add(it.id)
            }
        }
        toRemove.forEach {
            children.remove(it)
        }
        children.forEach { it.dedupe() }
    }
}