package me.mrkirby153.KirBot.command.tree

import net.dv8tion.jda.core.Permission
import java.lang.reflect.Method

class CommandNode(val name: String, var method: Method? = null, var instance: Any? = null,
                  var metadata: CommandNodeMetadata? = null) {

    private val children = mutableListOf<CommandNode>()

    fun isSkeleton(): Boolean {
        return method == null
    }

    fun getChild(name: String): CommandNode? {
        return children.firstOrNull { it.name == name }
    }

    fun addChild(node: CommandNode) {
        this.children.add(node)
    }

    fun removeChild(node: CommandNode) {
        this.children.remove(node)
    }

    fun removeChild(name: String) {
        this.children.removeIf { it.name == name }
    }

    fun getChildren(): List<CommandNode> {
        return children
    }
}

data class CommandNodeMetadata(val arguments: List<String>, val clearance: Int, val permissions: Array<Permission>, val admin: Boolean,
                               val ignoreWhitelist: Boolean, val log: Boolean)