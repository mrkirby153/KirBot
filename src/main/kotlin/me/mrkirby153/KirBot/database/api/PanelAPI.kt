package me.mrkirby153.KirBot.database.api

import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.User
import org.json.JSONObject

object PanelAPI {

    private val API_PROCESSOR: ApiProcessor = ApiProcessor(Bot.properties.getProperty("api-endpoint"), Bot.properties.getProperty("api-key"))

    private val apiProcessorThread: Thread = Thread(API_PROCESSOR).apply {
        name = "ApiProcessor"
        isDaemon = true
        start()
    }


    fun getRealname(user: User): ApiRequest<Realname> = object : ApiRequest<Realname>("/user/${user.id}/name") {
        override fun parse(json: JSONObject): Realname {
            return Realname(json.getString("first_name"), json.getString("last_name"))
        }
    }.apply {
        this.processor = API_PROCESSOR
    }

    fun getRealnames(users: List<User>): ApiRequest<Realnames> {
        return object : ApiRequest<Realnames>("/user/names", Methods.POST, mutableMapOf(Pair("names",
                users.map { it.id }.joinToString(",")))) {
            override fun parse(json: JSONObject): Realnames {
                val map = mutableMapOf<User, Realname>()

                users.forEach {
                    val obj = json.optJSONObject(it.id) ?: return@forEach
                    map[it] = Realname(obj.getString("first_name"), obj.getString("last_name"))
                }
                return Realnames(map)
            }
        }.apply {
            this.processor = API_PROCESSOR
        }
    }
}