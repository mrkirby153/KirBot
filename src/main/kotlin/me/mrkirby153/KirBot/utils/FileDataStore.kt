package me.mrkirby153.KirBot.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.mrkirby153.kcutils.utils.DataStore
import java.io.File

class FileDataStore<K, V>(private val file: File) : DataStore<K, V> {
    private var map = mutableMapOf<K, V>()

    private val gson = Gson()

    init {
        if (!file.exists()) {
            file.createNewFile()
            file.writeText("{}")
        }
        load()
    }

    override fun clear() {
        map.clear()
        save()
    }

    override fun containsKey(key: K) = map.containsKey(key)

    override fun containsValue(value: V) = map.containsValue(value)

    override fun get(key: K): V? = map[key]

    override fun remove(key: K): V? {
        val v = map.remove(key)
        save()
        return v
    }

    override fun set(key: K, value: V): V? {
        val v = map.put(key, value)
        save()
        return v
    }

    override fun size() = map.size

    fun save() {
        val json = gson.toJson(map)
        file.writer().use {
            it.write(json)
            it.flush()
        }
    }

    fun values() = map.values.toList()
    fun keys() = map.keys.toList()
    fun entries() = map.entries.toList()

    fun load() {
        val typeToken = object : TypeToken<Map<K, V>>() {}.type
        file.reader().use {
            map = gson.fromJson(it, typeToken)
        }
    }
}

