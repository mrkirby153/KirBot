package me.mrkirby153.KirBot.data

import com.google.gson.GsonBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.music.MusicData
import me.mrkirby153.KirBot.music.MusicManager
import me.mrkirby153.KirBot.server.ServerLogger
import me.mrkirby153.KirBot.server.data.DataRepository
import me.mrkirby153.KirBot.utils.CachedValue
import me.mrkirby153.KirBot.utils.child
import me.mrkirby153.KirBot.utils.mkdirIfNotExist
import net.dv8tion.jda.core.entities.TextChannel
import java.nio.charset.Charset

class ServerData(val id: Long, val shard: Shard) {

    private val guild = shard.getGuildById(id)


    val channelWhitelist = CachedValue<Array<String>>(1000 * 30)

    private val spamFilterDisabled = mutableMapOf<String, Long>()

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

    val musicManager = MusicManager(guild)
    val logger = ServerLogger(guild)

    fun getMusicData(): MusicData {
        return Database.getMusicData(guild)
    }

    fun disableSpamFilter(channel: TextChannel){
        spamFilterDisabled[channel.id] = System.currentTimeMillis() + 1000 *3600L // Re-enable the spam filter an hour from now
    }

    fun spamFilterEnabled(channel: TextChannel): Boolean {
        if(spamFilterDisabled.containsKey(channel.id)){
            val disabledUntil = spamFilterDisabled[channel.id] ?: return false
            if(System.currentTimeMillis() > disabledUntil){
                spamFilterDisabled.remove(channel.id)
                return true
            } else {
                return false
            }
        } else {
            return false
        }
    }
}