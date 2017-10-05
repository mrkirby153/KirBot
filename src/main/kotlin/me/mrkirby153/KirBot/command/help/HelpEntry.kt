package me.mrkirby153.KirBot.command.help

import org.json.JSONObject

open class HelpEntry() {
    var description = "No Description Provided"
    var name = "Unknown"

    constructor(name: String, description: String) : this() {
        this.name = name
        this.description = description
    }
}

class CommandHelp : HelpEntry() {

    val params = mutableListOf<HelpEntry>()

    val usage = mutableListOf<String>()


    fun deserialize(json: JSONObject) {
        this.description = json.optString("description") ?: "No description provided"
        this.name = json.getString("name")

        json.optJSONArray("params")?.let {
            it.map { it as JSONObject }.forEach {
                params.add(HelpEntry(it.getString("name"), it.getString("description")))
            }
        }

        json.optJSONArray("usage")?.let {
            usage.addAll(it.map { it as String })
        }
    }
}