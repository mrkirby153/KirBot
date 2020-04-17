package me.mrkirby153.KirBot.redis

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.logger.ErrorLogger
import net.dv8tion.jda.api.sharding.ShardManager
import org.json.JSONObject
import org.json.JSONTokener
import redis.clients.jedis.JedisPubSub

class RedisHandler(val shardManager: ShardManager) : JedisPubSub() {

    override fun onMessage(channel: String?, message: String?) {
        if (channel == null || message == null || channel != "kirbot")
            return
        try {
            val payload = JSONObject(JSONTokener(message))
            val cmd = payload.getString("command")

            val handler = RedisCommandManager.getCommand(cmd)
            if (handler == null) {
                Bot.LOG.warn("Unknown handler for command $cmd")
                return
            }
            val serverId = payload.optString("guild", null)
            val userId = payload.optString("user", null)
            val server = if (serverId != null) shardManager.getGuildById(serverId) else null
            val user = if (userId != null) shardManager.getUserById(userId) else null
            val body = payload.optJSONObject("data") ?: JSONObject()

            Bot.LOG.debug(
                    "Processing command $cmd for server $server with body $body. Sent by $userId")
            Bot.scheduler.submit {
                // Submit the task async as to not block the listener thread
                try {
                    handler.handle(server, user, body)
                } catch (e: Exception) {
                    Bot.LOG.error("Error processing command $cmd on $server - $body", e)
                }
            }
        } catch (e: Exception) {
            Bot.LOG.error("Error processing payload $message", e)
            ErrorLogger.logThrowable(e)
        }
    }
}