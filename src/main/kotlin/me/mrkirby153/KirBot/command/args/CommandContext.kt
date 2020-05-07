package me.mrkirby153.KirBot.command.args

@Suppress("UNCHECKED_CAST")
class CommandContext {

    private val arguments = mutableMapOf<String, Any?>()

    fun <T> get(key: String): T? = arguments[key] as T?

    fun <T> getNotNull(key: String): T = arguments[key] as T ?: throw NullPointerException(
            "key $key is null")

    fun put(key: String, `object`: Any?) = arguments.put(key, `object`)

    fun has(key: String) = arguments.containsKey(key)

    fun <T> ifPresent(key: String, action: (T) -> Unit) {
        val d = arguments[key] ?: return
        action.invoke(d as T)
    }

    override fun toString(): String {
        return "CommandContext(arguments=$arguments)"
    }

    fun getContextString(): String {
        return arguments.entries.joinToString(", ") { "${it.key}=${it.value}" }
    }

}