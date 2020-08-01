package com.mrkirby153.kirbot.services.command

import com.mrkirby153.kirbot.services.command.context.CommandParameter
import com.mrkirby153.kirbot.services.command.context.Parameter
import java.lang.reflect.Method

/**
 * Tree node for objects in the command tree
 */
class CommandNode(
        /**
         * The name of the command
         */
        val name: String,
        /**
         * The java method that backs this command
         */
        method: Method? = null,
        /**
         * The class of the backing method
         */
        var clazz: Class<*>? = null,
        /**
         * The instance to use when invoking the method
         */
        var instance: Any? = null) {

    /**
     * The parent command node
     */
    var parent: CommandNode? = null

    /**
     * A list of names of this node's parents
     */
    val parentNames: MutableList<String> = mutableListOf()

    var method: Method? = null
        set(value) {
            field = value
            parseArguments()
        }

    val commandParameters: MutableMap<String, CommandParameter> = mutableMapOf()

    private val children = mutableListOf<CommandNode>()

    val annotation: Command by lazy {
        if(method == null)
            throw IllegalArgumentException("Cannot get annotation of skeleton")
        method.getAnnotation(Command::class.java)
    }

    val fullName: String
        get() = buildString {
            if(parentNames.isNotEmpty()) {
                append(parentNames.joinToString(" "))
                append(" ")
            }
            append(name)
        }.trim()

    init {
        this.method = method
    }

    /**
     * Checks if this command is a skeleton node
     *
     * @return True if this command is a skeleton node
     */
    fun isSkeleton() = method == null

    /**
     * Gets a 1st level child
     *
     * @param name The name of thie child (case insensitive)
     * @return The child command node
     */
    fun getChild(name: String) = children.firstOrNull { it.name.equals(name, true) }

    /**
     * Adds a child to this command node. The child node's parent is automatically updated to this
     * command node
     *
     * @param node The node to add.
     */
    fun addChild(node: CommandNode) {
        this.children.add(node)
        val newParents = parentNames.toMutableList()
        newParents.add(name)
        node.parent = this
        node.parentNames.clear()
        node.parentNames.addAll(newParents)
    }

    override fun equals(other: Any?): Boolean {
        other ?: return false
        if (this === other) return true

        if (other !is CommandNode) return false

        if (isSkeleton())
        // Skeleton commands are equal if their name is the same and they share the same parent
            return other.name == this.name && other.parent == this.parent

        // Commands that have the same backing method and clazz are equal
        return other.method == this.method && other.clazz == this.clazz
    }

    private fun parseArguments() {
        val m = method ?: return
        commandParameters.clear()
        var index = 0
        m.parameters.forEach {
            val name = it.getAnnotation(Parameter::class.java)?.value ?: it.name
            commandParameters[name] = CommandParameter(it.type, it, name, index, m.parameterCount)
            index++
        }
    }
}