package me.mrkirby153.KirBot.music

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.api.MusicSettings
import me.mrkirby153.KirBot.database.api.PanelAPI
import net.dv8tion.jda.core.entities.Guild
import java.util.concurrent.TimeUnit

class MusicManager(val server: Guild) {

    val audioPlayer: AudioPlayer = Bot.playerManager.createPlayer()

    val trackScheduler: TrackScheduler = TrackScheduler(audioPlayer, server)

    val audioSender: AudioSender = AudioSender(audioPlayer)

    var adminOnly = false

    init {
        audioPlayer.addListener(trackScheduler)
    }

    companion object {
        val musicSettings = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build(
                object : CacheLoader<String, MusicSettings?>() {
                    override fun load(p0: String): MusicSettings? {
                        val guid = Bot.getGuild(p0) ?: return null
                        return PanelAPI.getMusicSettings(guid).execute()
                    }
                }
        )
    }

}
