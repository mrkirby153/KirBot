package me.mrkirby153.KirBot.command.help

import me.mrkirby153.KirBot.Bot
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File

class HelpManager {

    val commands = mutableListOf<CommandHelp>()

    val FILE_URI = this.javaClass.getResource("/commands.json").toURI()

    fun load() {
        Bot.LOG.info("Started loading help from \"$FILE_URI\"")
        val file = File(FILE_URI)

        val inputStream = file.inputStream()

        val jsonArray = JSONArray(JSONTokener(inputStream))
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