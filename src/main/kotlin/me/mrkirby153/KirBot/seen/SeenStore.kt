package me.mrkirby153.KirBot.seen

import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import org.json.JSONObject
import org.json.JSONTokener

class SeenStore {

    fun serialize(data: SeenData) = JSONObject().apply {
        put("user", data.user)
        put("server", data.server)
        put("lastMessage", data.lastMessage)
        put("status", data.status.key)
    }

    fun deserialize(data: JSONObject) = SeenData(data.getString("user"), data.getString("server"), data.getLong("lastMessage"),
            OnlineStatus.fromKey(data.getString("status")))

    fun get(user: User): SeenData? {
        ModuleManager[Redis::class].redisConnection.get().use {
            if (it.get("seen.${user.id}") == null)
                return null
            val json = JSONObject(JSONTokener(it.get("seen.${user.id}")))
            return deserialize(json)
        }
    }

    fun update(user: User, server: Guild) {
        val data = get(user) ?: SeenData(user.id, "", -1, OnlineStatus.UNKNOWN)
        data.server = server.name
        data.lastMessage = System.currentTimeMillis()
        data.status = user.getMember(server).onlineStatus
        set(user, data)
    }

    private fun set(user: User, data: SeenData) {
        ModuleManager[Redis::class].redisConnection.get().use {
            it.set("seen.${user.id}", serialize(data).toString())
        }
    }

    fun updateOnlineStatus(member: Member) {
        val data = get(member.user) ?: SeenData(member.user.id, "", -1, OnlineStatus.UNKNOWN)
        data.status = member.onlineStatus
        set(member.user, data)
    }

    class SeenData(val user: String, var server: String, var lastMessage: Long, var status: OnlineStatus)
}