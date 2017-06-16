package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.utils.Context


abstract class MessageProcessor(val startSequence: String, val endSequence: String) {

    lateinit var shard: Shard

    lateinit var guildData: ServerData

    lateinit var matches: Array<String>

    var stopProcessing = false

    abstract fun process(context: Context)
}