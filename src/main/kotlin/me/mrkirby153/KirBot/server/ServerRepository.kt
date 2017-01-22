package me.mrkirby153.KirBot.server

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.child
import me.mrkirby153.KirBot.utils.mkdirIfNotExist
import net.dv8tion.jda.core.entities.Guild

object ServerRepository {

    val servers = mutableMapOf<String, Server>()

    val serverDirectory = Bot.files.data.child("servers").mkdirIfNotExist()

    val gson: Gson = GsonBuilder().run {
        setPrettyPrinting()
        create()
    }

    fun getServer(guild: Guild): Server? {
        if (servers[guild.id] == null) {
            servers[guild.id] = Server(guild)
        }
        return servers[guild.id]
    }
}