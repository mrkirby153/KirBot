package com.mrkirby153.kirbot.services.command

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
        var method: Method? = null,
        /**
         * The class of the backing method
         */
        var clazz: Class<*>? = null) {

    /**
     * The parent command node
     */
    var parent: CommandNode? = null

    private val children = mutableListOf<CommandNode>()

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
        node.parent = this
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
}