package me.mrkirby153.KirBot.command.help

import me.mrkirby153.KirBot.Bot
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class HelpManager {

    val commands = mutableListOf<CommandHelp>()

    private val INPUT_STREAM = this.javaClass.getResourceAsStream("/commands.json")!!

    fun load() {
        Bot.LOG.info("Started loading help from \"$INPUT_STREAM\"")

        val jsonArray = JSONArray(JSONTokener(INPUT_STREAM))
        Bot.LOG.debug("There are ${jsonArray.length()} command help entries registered")

        jsonArray.map { it as JSONObject }.forEach {
            val help = CommandHelp()
            help.deserialize(it)
            Bot.LOG.debug("Loaded help for ${help.name}")
            commands.add(help)
        }
        Bot.LOG.info("Registered help for ${commands.size} commands")
    }

    fun get(command: String) = commands.firstOrNull { it.name.equals(command, true) }
}