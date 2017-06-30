package me.mrkirby153.KirBot.command.args

@Suppress("UNCHECKED_CAST")
class CommandContext {

    private val parsed = mutableMapOf<String, Any>()

    fun <T> get(key: String): T? {
        return parsed[key] as T?
    }

    fun put(key: String, obj: Any) {
        parsed.put(key, obj)
    }

    fun has(key: String): Boolean {
        return parsed[key] != null
    }

    fun <T> has(key: String, data: (T) -> Unit) {
        val d = parsed[key] ?: return
        data.invoke(d as T)
    }

    fun number(key: String): Double? {
        return get<Double>(key)
    }

    fun string(key: String): String? {
        return get<String>(key)
    }
}