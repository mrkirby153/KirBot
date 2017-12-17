package me.mrkirby153.KirBot.command.args

import java.util.*

class ArgumentList(args : Array<String>) {

    private val arguments = LinkedList<String>()

    init {
        arguments.addAll(args)
    }

    fun popFirst() = arguments.pop()

    fun peek() = arguments.peek()

    fun hasNext() = arguments.isNotEmpty()
}