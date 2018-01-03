package me.mrkirby153.KirBot.scheduler

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type

class InterfaceAdapter<T : Any> : JsonSerializer<T>, JsonDeserializer<T> {

    override fun serialize(obj: T, interfaceType: Type,
                           context: JsonSerializationContext): JsonElement {
        val wrapper = JsonObject()
        wrapper.addProperty("type", obj.javaClass.name)
        wrapper.add("data", context.serialize(obj))
        return wrapper
    }

    @Throws(JsonParseException::class)
    override fun deserialize(elem: JsonElement, interfaceType: Type,
                             context: JsonDeserializationContext): T {
        val wrapper = elem as JsonObject
        val typeName = get(wrapper, "type")
        val data = get(wrapper, "data")
        val actualType = typeForName(typeName)
        return context.deserialize(data, actualType)
    }

    private fun typeForName(typeElem: JsonElement): Type {
        try {
            return Class.forName(typeElem.asString)
        } catch (e: ClassNotFoundException) {
            throw JsonParseException(e)
        }

    }

    private fun get(wrapper: JsonObject, memberName: String): JsonElement {
        return wrapper.get(memberName) ?: throw JsonParseException(
                "no '$memberName' member found in what was expected to be an interface wrapper")
    }
}