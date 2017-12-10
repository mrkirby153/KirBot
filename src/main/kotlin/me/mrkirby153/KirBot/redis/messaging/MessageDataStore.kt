package me.mrkirby153.KirBot.redis.messaging

import me.mrkirby153.KirBot.database.api.ServerMessage
import me.mrkirby153.KirBot.redis.RedisConnector
import org.json.JSONObject
import org.json.JSONTokener

class MessageDataStore {

    private val LIST_KEY = "messages"

    fun getMessageContent(id: String, callback: (msg: Message?) -> Unit) {
        RedisConnector.get().use {
            val listSize = it.llen(LIST_KEY)

            val list = it.lrange(LIST_KEY, 0, listSize).map { JSONObject(JSONTokener(it)) }.map { decode(it) }

            val messages = list.filter { it.id == id }

            if (messages.isEmpty()) {
                ServerMessage.get(id).queue {
                    callback.invoke(Message(it.id ?: "-1", it.serverId, it.authorId, it.channelId, it.content))
                }
                return
            }

            callback.invoke(messages[messages.lastIndex])
        }
    }

    fun pushMessage(message: net.dv8tion.jda.core.entities.Message) {
        RedisConnector.get().use {
            val json = encode(Message(message.id, message.guild.id, message.author.id, message.channel.id, message.content, message.creationTime.toEpochSecond()))
            it.rpush(LIST_KEY, json.toString())
        }
    }

    fun deleteMessage(messageId: String, callback: ((msg: String) -> Unit)?) {
        RedisConnector.get().use {
            val msgs = it.llen(LIST_KEY)
            val msgList = it.lrange(LIST_KEY, 0, msgs)
            msgList.forEach { msg ->
                val decoded = decode(JSONObject(JSONTokener(msg)))
                if (decoded.id == messageId) {
                    callback?.invoke(decoded.message)
                    it.lrem(LIST_KEY, 0, msg)
                    return
                }
            }
        }
        ServerMessage.delete(messageId).queue {
            callback?.invoke(it.content)
        }
    }

    fun bulkDelete(messageIds: Array<String>, callback: (() -> Unit)?) {
        val deleted = mutableListOf<String>()
        RedisConnector.get().use {
            val msgList = it.lrange(LIST_KEY, 0, it.llen(LIST_KEY))
            msgList.forEach { msg ->
                val decoded = decode(JSONObject(JSONTokener(msg)))
                deleted.add(decoded.id)
                if (decoded.id in messageIds)
                    it.lrem(LIST_KEY, 0, msg)
            }
        }
        val toDelete = messageIds.clone().toMutableList()
        toDelete.removeAll(deleted)
        println(toDelete)
        if (toDelete.isNotEmpty())
            ServerMessage.bulkDelete(toDelete).queue {
                callback?.invoke()
            }
        else
            callback?.invoke()
    }


    fun encode(message: Message): JSONObject = JSONObject().apply {
        put("id", message.id)
        put("author", message.author)
        put("channel", message.channel)
        put("message", message.message)
        put("server", message.serverId)
        put("time", message.time)
    }

    fun decode(obj: JSONObject): Message = Message(obj.getString("id"), obj.getString("server"), obj.getString("author"), obj.getString("channel"), obj.getString("message"), obj.optLong("time"))


    data class Message(val id: String, val serverId: String, val author: String, val channel: String, val message: String, val time: Long? = System.currentTimeMillis())
}