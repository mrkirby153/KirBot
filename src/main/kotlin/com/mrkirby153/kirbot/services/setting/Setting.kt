package com.mrkirby153.kirbot.services.setting

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

/**
 * Abstract class representing a guild setting
 */
abstract class Setting<T>(
        /**
         * The key of the setting
         */
        val key: String,

        /**
         * The default value of the setting
         */
        val default: T? = null) {

    /**
     * Deserialize a setting from the database
     *
     * @param raw The setting value from the database
     * @return The deserialized setting
     */
    abstract fun deserialize(raw: String): T

    /**
     * Serializes the setting for storage into the database
     *
     * @param data The data to serialize
     * @return The serialized setting
     */
    abstract fun serialize(data: T): String
}

/**
 * A string setting
 */
class StringSetting(key: String, default: String? = null) : Setting<String>(key, default) {

    override fun deserialize(raw: String) = raw

    override fun serialize(data: String) = data

}

/**
 * A number setting. Numbers are always stored as longs
 */
class NumberSetting(key: String, default: Long? = null) : Setting<Long>(key, default) {

    override fun deserialize(raw: String) = raw.toLong()

    override fun serialize(data: Long) = data.toString()

}

/**
 * A boolean setting. Booleans are represented in the database as the numbers 1 and 0 for true
 * and false respectively
 */
class BooleanSetting(key: String, default: Boolean? = null) : Setting<Boolean>(key, default) {

    override fun deserialize(raw: String) = raw == "true" || raw == "1"

    override fun serialize(data: Boolean) = if (data) "1" else "0"
}

/**
 * A json array setting. Data is returned as an array of java objects for convenience. Object
 * serialization and deserialization is handled by Jackson
 */
private val mapper = jacksonObjectMapper()

class ArraySetting<T>(key: String, private val clazz: Class<T>, default: Array<T>? = null) :
        Setting<Array<T>>(key, default) {
    override fun deserialize(raw: String): Array<T> {
        val type = mapper.typeFactory.constructArrayType(clazz)
        return mapper.readValue(raw, type)
    }

    override fun serialize(data: Array<T>): String = mapper.writeValueAsString(data)
}

/**
 * A json object setting represented as a java object for convenience. Object serialization and
 * deserialization is handled by Jackson
 */
class ObjectSetting<T>(key: String, private val clazz: Class<T>, default: T? = null) :
        Setting<T>(key, default) {

    override fun serialize(data: T): String = mapper.writeValueAsString(data)

    override fun deserialize(raw: String): T = mapper.readValue<T>(raw, clazz)
}