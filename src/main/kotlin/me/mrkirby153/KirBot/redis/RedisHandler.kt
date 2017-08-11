package me.mrkirby153.KirBot.redis

import me.mrkirby153.KirBot.redis.commands.RedisCommandManager
import org.json.JSONObject
import org.json.JSONTokener
import redis.clients.jedis.JedisPubSub

class RedisHandler : JedisPubSub() {

    override fun onPMessage(pattern: String, channel: String, message: String) {
        if (!channel.startsWith("kirbot")) {
            return
        }

        val action = channel.split(":")[1]

        val json = JSONObject(JSONTokener(message))

        RedisCommandManager.getCommand(action)?.handle(json)
    }
}