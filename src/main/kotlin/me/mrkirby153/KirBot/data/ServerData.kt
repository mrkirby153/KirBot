package me.mrkirby153.KirBot.data

import com.google.gson.GsonBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.server.data.DataRepository
import me.mrkirby153.KirBot.sharding.Shard
import me.mrkirby153.kcutils.child
import me.mrkirby153.kcutils.mkdirIfNotExist
import java.nio.charset.Charset

class ServerData(val id: Long, val shard: Shard) {

    private val guild = shard.getGuildById(id)


    private val gson = with(GsonBuilder()) {
        setPrettyPrinting()
    }.create()

    val repository: DataRepository by lazy {
        val fileName = "$id.json"
        val file = Bot.files.data.child("servers").mkdirIfNotExist().child(fileName)
        if (!file.exists()) {
            DataRepository(guild)
        } else {
            val reader = file.reader(Charset.defaultCharset())
            val server = gson.fromJson(reader, DataRepository::class.java)
            server.server = guild
            reader.close()
            server
        }
    }

    val logManager = LogManager(guild)

    val musicManager = me.mrkirby153.KirBot.music.MusicManager(guild)
}