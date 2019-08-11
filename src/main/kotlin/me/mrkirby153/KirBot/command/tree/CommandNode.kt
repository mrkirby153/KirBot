package me.mrkirby153.KirBot.command.tree

import me.mrkirby153.KirBot.command.CommandCategory
import net.dv8tion.jda.api.Permission
import java.lang.reflect.Method

class CommandNode(val name: String, var method: Method? = null,
                  var instance: Any? = null,
                  var metadata: CommandNodeMetadata? = null) {

    private val children = mutableListOf<CommandNode>()

    var parentString: String = ""

    var rootNode = false

    val aliases = mutableListOf<String>()

    fun isSkeleton(): Boolean {
        return method == null
    }

    fun getChild(name: String): CommandNode? {
        return children.firstOrNull {
            it.name.equals(name,
                    true) || name.toLowerCase() in it.aliases.map { alias -> alias.toLowerCase() }
        }
    }

    fun addChild(node: CommandNode) {
        if (!rootNode)
            node.parentString = "$parentString $name"
        this.children.add(node)
    }

    fun removeChild(node: CommandNode) {
        this.children.remove(node)
    }

    fun removeChild(name: String) {
        this.children.removeIf { it.name.equals(name, true) }
    }

    fun getChildren(): List<CommandNode> {
        return children
    }

    fun getLeaves(): List<CommandNode> {
        val l = mutableListOf<CommandNode>()
        if (!this.isSkeleton()) {
            l.add(this)
        }
        this.children.forEach { child ->
            l.addAll(child.getLeaves())
        }
        return l
    }

    override fun toString(): String {
        val name = "$name/${this.aliases.joinToString("/")}"
        return "CommandNode(name='$name', parentString='$parentString')"
    }


}

data class CommandNodeMetadata(val arguments: List<String>, val clearance: Int,
                               val permissions: Array<Permission>, val admin: Boolean,
                               val ignoreWhitelist: Boolean, val log: Boolean,
                               val description: String?,
                               val category: CommandCategory = CommandCategory.UNCATEGORIZED)