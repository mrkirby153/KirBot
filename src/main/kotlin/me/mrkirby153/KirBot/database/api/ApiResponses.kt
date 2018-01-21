package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import org.json.JSONArray
import org.json.JSONObject
import java.sql.Timestamp

enum class ChannelType {
    VOICE,
    TEXT
}

class MusicSettings(val enabled: Boolean, whitelist: String, val channels: Array<String>,
                    val blacklistedSongs: Array<String>,
                    val maxQueueLength: Int, val playlists: Boolean, val maxSongLength: Int,
                    val skipCooldown: Int, val skipTimer: Int) {

    val whitelistMode = WhitelistMode.valueOf(whitelist)

    companion object {
        fun get(guid: Guild) = object : ApiRequest<MusicSettings>("/server/${guid.id}/music") {
            override fun parse(json: JSONObject): MusicSettings {
                return MusicSettings(json.getInt("enabled") == 1, json.getString("mode"),
                        json.getJSONArray("channels").map { it.toString() }.toTypedArray(),
                        arrayOf(""), json.getInt("max_queue_length"),
                        json.getInt("playlists") == 1, json.getInt("max_song_length"),
                        json.getInt("skip_cooldown"), json.getInt("skip_timer"))
            }
        }
    }

    enum class WhitelistMode {
        OFF,
        BLACKLIST,
        WHITELIST
    }
}

class ServerMessage(val id: String?, val channelId: String, val serverId: String,
                    val authorId: String, val content: String) {
    val guild = Bot.shardManager.getGuild(this.serverId)
    val channel = guild?.getTextChannelById(this.channelId)
    val author = Bot.shardManager.getUser(this.authorId)

    companion object {
        fun get(id: String): ApiRequest<ServerMessage> = object :
                ApiRequest<ServerMessage>("/message/$id") {
            override fun parse(json: JSONObject): ServerMessage {
                return ServerMessage(json.getString("id"), json.getString("channel"),
                        json.getString("server_id"), json.getString("author"),
                        json.getString("message"))
            }
        }

        fun delete(id: String) = object :
                ApiRequest<ServerMessage>("/message/$id", Methods.DELETE) {
            override fun parse(json: JSONObject): ServerMessage {
                return if (json.keySet().size < 1)
                    ServerMessage("-1", "-1", "-1", "-1", "-1")
                else
                    ServerMessage(json.getString("id"), json.getString("channel"),
                            json.getString("server_id"), json.getString("author"),
                            json.getString("message"))
            }
        }

        fun bulkDelete(messages: Collection<String>) = object :
                ApiRequest<Void>("/message/bulkDelete", Methods.DELETE,
                        mapOf(Pair("messages", messages.joinToString(",")))) {}
    }
}

class GuildRole(val id: String, val name: String, val serverId: String, val permissions: Long) {
    val guild = Bot.shardManager.getGuild(serverId)
    val role = guild?.getRoleById(id)

    fun delete() = object : ApiRequest<Void>("/role/$id", Methods.DELETE) {}

    fun update() = object : ApiRequest<GuildRole>("/role/${role!!.id}",
            Methods.PATCH,
            mapOf(Pair("name", role.name), Pair("permissions", role.permissionsRaw.toString()))) {
        override fun parse(json: JSONObject): GuildRole {
            return GuildRole(json.getString("id"), json.getString("name"),
                    json.getString("server_id"),
                    json.getLong("permissions"))
        }
    }

    companion object {
        fun create(role: net.dv8tion.jda.core.entities.Role) = object :
                ApiRequest<GuildRole>("/role", Methods.POST,
                        mapOf(Pair("id", role.id), Pair("server_id", role.guild!!.id),
                                Pair("name", role.name),
                                Pair("permissions", role.permissionsRaw.toString()))) {
            override fun parse(json: JSONObject): GuildRole {
                return GuildRole(json.getString("id"), json.getString("name"),
                        json.getString("server_id"), json.getLong("permissions"))
            }
        }

        fun get(role: net.dv8tion.jda.core.entities.Role) = object :
                ApiRequest<GuildRole>("/role/${role.id}", Methods.GET) {
            override fun parse(json: JSONObject): GuildRole {
                return GuildRole(json.getString("id"), json.getString("name"),
                        json.getString("server_id"),
                        json.getLong("permissions"))
            }
        }

        fun delete(id: String) = object : ApiRequest<Void>("/role/$id", Methods.DELETE) {}
    }
}

class Quote(val id: Int, val messageId: String, val user: String, val server: String,
            val content: String) {
    val guild = Bot.shardManager.getGuild(server)

    companion object {
        fun create(message: Message) = object : ApiRequest<Quote>("/server/quote",
                Methods.PUT,
                mapOf(Pair("server_id", message.guild.id), Pair("user", message.author.name),
                        Pair("content", message.content), Pair("message_id", message.id))) {
            override fun parse(json: JSONObject): Quote {
                return Quote(json.getInt("id"), json.getString("message_id"),
                        json.getString("user"), json.getString("server_id"),
                        json.getString("content"))
            }
        }

        fun get(id: String) = object : ApiRequest<Quote?>("/server/quote/$id") {
            override fun parse(json: JSONObject): Quote? {
                if (!json.has("id")) {
                    return null
                }
                return Quote(json.getInt("id"), json.getString("message_id"),
                        json.getString("user"),
                        json.getString("server_id"), json.getString("content"))
            }
        }

        fun get(guild: Guild): ApiRequest<Array<Quote>> = object :
                ApiRequest<Array<Quote>>("/server/${guild.id}/quotes") {
            override fun parse(json: JSONArray): Array<Quote> {
                val quotes = mutableListOf<Quote>()
                json.forEach { q ->
                    if (q is JSONObject) {
                        quotes.add(Quote(q.getInt("id"), q.getString("message_id"),
                                q.getString("user"),
                                q.getString("server_id"), q.getString("content")))
                    }
                }
                return quotes.toTypedArray()
            }
        }
    }
}

class ClearanceOverride(val id: Int, val command: String, cl: String) {
    var clearance = Clearance.valueOf(cl)

    fun update(): ApiRequest<ClearanceOverride> {
        return object : ApiRequest<ClearanceOverride>("/overrides/$id", Methods.PATCH,
                mapOf(Pair("clearance", clearance.toString()))) {
            override fun parse(json: JSONObject): ClearanceOverride {
                return this@ClearanceOverride
            }
        }
    }

    fun delete(): ApiRequest<Void> {
        return object : ApiRequest<Void>("/overrides/$id", Methods.DELETE) {}
    }

    companion object {
        fun create(guild: Guild, command: String, clearance: Clearance) = object :
                ApiRequest<ClearanceOverride>("/server/${guild.id}/overrides", Methods.PUT,
                        mapOf(Pair("command", command), Pair("clearance", clearance.toString()))) {
            override fun parse(json: JSONObject): ClearanceOverride {
                return ClearanceOverride(json.getInt("id"), json.getString("command"),
                        json.getString("clearance"))
            }

        }

        fun get(guild: Guild): ApiRequest<MutableList<ClearanceOverride>> = object :
                ApiRequest<MutableList<ClearanceOverride>>("/server/${guild.id}/overrides") {
            override fun parse(json: JSONArray): MutableList<ClearanceOverride> {
                val overrides = mutableListOf<ClearanceOverride>()
                json.map { it as JSONObject }.forEach { j ->
                    overrides.add(ClearanceOverride(j.getInt("id"), j.getString("command"),
                            j.getString("clearance")))
                }
                return overrides
            }
        }
    }
}

class RssFeed(val id: String, val channelId: String, val serverId: String, val url: String,
              val failed: Boolean, val lastCheck: Timestamp?) {

    val guild: Guild?
        get() = Bot.shardManager.getGuild(serverId)

    val channel: TextChannel?
        get() = guild?.getTextChannelById(this.channelId)

    val items = mutableSetOf<FeedItem>()

    data class FeedItem(val guid: String)

    fun delete() = object : ApiRequest<Void>("/feed/$id", Methods.DELETE) {}

    fun update(success: Boolean) = object :
            ApiRequest<Void>("/feed/server/$serverId/check", Methods.PATCH,
                    mapOf(Pair("feed", id), Pair("success", success.toString()))) {}

    fun posted(guid: String) {
        val req = object :
                ApiRequest<FeedItem>("/feed/$id/item", Methods.PUT, mapOf(Pair("guid", guid))) {
            override fun parse(json: JSONObject): FeedItem {
                return FeedItem(json.getString("guid"))
            }
        }
        req.queue({
            items.add(it)
        })
    }

    fun isPosted(guid: String) = items.contains(FeedItem(guid))

    companion object {
        fun get(id: String) = object : ApiRequest<RssFeed>("/feed/$id") {
            override fun parse(json: JSONObject): RssFeed {
                return parseFeed(json)
            }
        }

        fun get(guild: Guild) = object : ApiRequest<Array<RssFeed>>("/feed/server/${guild.id}") {
            override fun parse(json: JSONArray): Array<RssFeed> {
                val list = mutableListOf<RssFeed>()
                json.map { it as JSONObject }.forEach {
                    list.add(parseFeed(it))
                }
                return list.toTypedArray()
            }
        }

        fun parseFeed(json: JSONObject): RssFeed {
            val timestamp: Timestamp? = if (!json.isNull("lastCheck")) {
                Timestamp(json.getLong("lastCheck") * 1000)
            } else {
                null
            }
            val rssFeed = RssFeed(json.getString("id"), json.getString("channel_id"),
                    json.getString("server_id"), json.getString("feed_url"),
                    json.optBoolean("failed", false), timestamp)

            if (json.has("items")) {
                json.getJSONArray("items").map { it as JSONObject }.forEach {
                    rssFeed.items.add(RssFeed.FeedItem(it.getString("guid")))
                }
            }
            return rssFeed
        }

        fun create(url: String, channel: String, guild: Guild) = object :
                ApiRequest<RssFeed>("/feed/server/${guild.id}", Methods.PUT,
                        mapOf(Pair("channel_id", channel), Pair("feed_url", url))) {
            override fun parse(json: JSONObject): RssFeed {
                return parseFeed(json)
            }
        }
    }
}

