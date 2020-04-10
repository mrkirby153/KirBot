package me.mrkirby153.KirBot.utils.settings

import net.dv8tion.jda.api.entities.Guild
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

abstract class Setting<T>(private val key: String, private val default: T? = null) {

    fun nullableGet(guildId: String): T? {
        val setting = SettingsRepository.get(guildId, key)
        return if (setting == null) {
            this.default
        } else {
            parse(stripLeadingQuotes(setting))
        }
    }

    fun get(guildId: String) = nullableGet(guildId)!!

    fun get(guild: Guild) = get(guild.id)

    fun nullableGet(guild: Guild) = nullableGet(guild.id)

    fun set(guildId: String, value: T?) {
        val toSet = if (value == null) null else toJson(value)
        SettingsRepository.set(guildId, key, toSet)
    }

    fun set(guild: Guild, value: T?) = set(guild.id, value)

    abstract fun parse(rawJson: String): T

    open fun toJson(value: T): String {
        return value.toString()
    }

    private fun stripLeadingQuotes(string: String): String = string.replace(Regex("(^\"|\"\$)"), "")
}

class StringSetting(key: String, default: String? = null) : Setting<String>(key, default) {
    override fun parse(rawJson: String): String {
        return rawJson
    }
}

class NumberSetting(key: String, default: Long? = null) : Setting<Long>(key, default) {

    override fun parse(rawJson: String): Long {
        return rawJson.toLong()
    }

}

class BooleanSetting(key: String, default: Boolean? = null) : Setting<Boolean>(key, default) {
    override fun parse(rawJson: String): Boolean {
        return rawJson == "true" || rawJson == "1"
    }

    override fun toJson(value: Boolean): String {
        return if (value) "1" else "0"
    }
}

class JsonArraySetting(key: String, default: JSONArray? = null) : Setting<JSONArray>(key, default) {

    override fun parse(rawJson: String): JSONArray {
        return JSONArray(JSONTokener(rawJson))
    }
}

class JsonObjectSetting(key: String, default: JSONObject? = null) : Setting<JSONObject>(key, default) {
    override fun parse(rawJson: String): JSONObject {
        return JSONObject(JSONTokener(rawJson))
    }
}