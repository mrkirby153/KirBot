package me.mrkirby153.KirBot.server.data

import com.google.gson.GsonBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.child
import me.mrkirby153.KirBot.utils.mkdirIfNotExist
import net.dv8tion.jda.core.entities.Guild

private val GSON = GsonBuilder().setPrettyPrinting().create()

class DataRepository(@Transient var server: Guild) {

    private val data = mutableMapOf<String, String>()

    fun <T> get(type: Class<T>, key: String): T? {
        if (!data.containsKey(key))
            return null
        return GSON.fromJson(data[key], type)
    }

    fun getBoolean(key: String): Boolean? {
        return get(Boolean::class.java, key)
    }

    fun getString(key: String, default: String?): String? {
        return get(String::class.java, key) ?: default
    }

    fun put(key: String, value: Any) {
        data.put(key, GSON.toJson(value))
        save()
    }

    fun remove(key: String) {
        data.remove(key)
        save()
    }

    fun save() {
        val fileName = "${server.id}.json"
        val file = Bot.files.data.child("servers").mkdirIfNotExist().child(fileName)
        val json = GSON.toJson(this)
        file.writeText(json)
    }
}