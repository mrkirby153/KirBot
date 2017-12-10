package me.mrkirby153.KirBot.redis.messaging

import me.mrkirby153.KirBot.database.api.ServerMessage
import me.mrkirby153.KirBot.redis.RedisConnector
import org.json.JSONObject
import org.json.JSONTokener

class MessageDataStore {

    private val LIST_KEY = "messages"

    private val DATA_KEY_PREFIX = "message"

    fun getMessageContent(id: String, callback: (msg: Message?) -> Unit) {
        RedisConnector.get().use {
            val msg = getMessageFromRedis(id)
            if (msg != null)
                callback.invoke(msg)
            else {
                ServerMessage.get(id).queue {
                    callback.invoke(Message(it.id ?: "-1", it.serverId, it.authorId, it.channelId,
                            it.content))
                }
            }
        }
    }

    fun storeMessage(message: net.dv8tion.jda.core.entities.Message): String {
        val key = "$DATA_KEY_PREFIX:${message.id}"
        RedisConnector.get().use {
            it.set(key, encode(Message(message)).toString())
            return key
        }
    }

    fun getMessageFromRedis(key: String): Message? {
        RedisConnector.get().use {
            val json = it.get("$DATA_KEY_PREFIX:$key") ?: return null
            val obj = JSONObject(JSONTokener(json))
            return decode(obj)
        }
    }

    fun pushMessage(message: net.dv8tion.jda.core.entities.Message) {
        RedisConnector.get().use {
            it.rpush(LIST_KEY, storeMessage(message))
        }
    }

    fun deleteMessage(messageId: String, callback: ((msg: String) -> Unit)?) {
        val msg = getMessageFromRedis(messageId)
        if (msg != null) { // Message is queued in redis
            RedisConnector.get().use {
                it.lrem(LIST_KEY, 0, "$DATA_KEY_PREFIX:$messageId")
                it.del("$DATA_KEY_PREFIX:$messageId")
                callback?.invoke(msg.message)
                return
            }
        } else {
            ServerMessage.delete(messageId).queue {
                callback?.invoke(it.content)
            }
        }
    }

    fun bulkDelete(messageIds: Array<String>, callback: (() -> Unit)?) {
        val deleted = mutableListOf<String>()
        RedisConnector.get().use {
            messageIds.forEach { id ->
                if (getMessageFromRedis(id) != null) {
                    it.lrem(LIST_KEY, 0, "$DATA_KEY_PREFIX:$id")
                    it.del("$DATA_KEY_PREFIX:$id")
                    deleted.add(id)
                }
            }
        }
        val toDelete = messageIds.clone().filter { it !in deleted }
        if (toDelete.isNotEmpty()) {
            ServerMessage.bulkDelete(toDelete).queue {
                callback?.invoke()
            }
        } else {
            callback?.invoke()
        }
    }


    private fun encode(message: Message): JSONObject = JSONObject().apply {
        put("id", message.id)
        put("author", message.author)
        put("channel", message.channel)
        put("message", message.message)
        put("server", message.serverId)
        put("time", message.time)
    }

    private fun decode(obj: JSONObject): Message = Message(obj.getString("id"), obj.getString("server"),
            obj.getString("author"), obj.getString("channel"), obj.getString("message"),
            obj.optLong("time"))


    data class Message(val id: String, val serverId: String, val author: String,
                       val channel: String, val message: String,
                       val time: Long? = System.currentTimeMillis()) {
        constructor(msg: net.dv8tion.jda.core.entities.Message) : this(msg.id, msg.guild.id,
                msg.author.id, msg.channel.id, msg.content, msg.creationTime.toEpochSecond())
    }
}