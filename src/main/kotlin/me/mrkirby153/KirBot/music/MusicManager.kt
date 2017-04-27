package me.mrkirby153.KirBot.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.server.Server

class MusicManager(val server: Server) {

    val audioPlayer: AudioPlayer = Bot.playerManager.createPlayer()

    val trackScheduler: TrackScheduler = TrackScheduler(audioPlayer, server)

    val audioSender: AudioSender = AudioSender(audioPlayer)

    var adminOnly = false

    init {
        audioPlayer.addListener(trackScheduler)
    }

}
