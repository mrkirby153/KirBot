package com.mrkirby153.kirbot.redis

/**
 * Event fired when a message is received over redis. [key] is the sub-key of the message
 * (i.e. `kirbot:x`) and [body] is the body of the message.
 */
data class RedisMessageEvent(val key: String, val body: ByteArray) {

    /**
     * The body as a [String]. This value is lazily computed
     */
    val bodyStr by lazy {
        String(body)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RedisMessageEvent

        if (key != other.key) return false
        if (!body.contentEquals(other.body)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + body.contentHashCode()
        return result
    }
}