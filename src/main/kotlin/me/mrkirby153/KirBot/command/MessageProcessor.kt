package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.server.Server
import net.dv8tion.jda.core.entities.Message


abstract class MessageProcessor(val startSequence: String, val endSequence: String) {

    lateinit var server: Server

    lateinit var matches: Array<String>

    var stopProcessing = false

    abstract fun process(message: Message)
}