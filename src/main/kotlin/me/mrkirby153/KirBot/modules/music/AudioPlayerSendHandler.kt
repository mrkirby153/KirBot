package me.mrkirby153.KirBot.modules.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer

class AudioPlayerSendHandler(val player: AudioPlayer): AudioSendHandler {

    private var lastFrame: AudioFrame? = null

    override fun provide20MsAudio(): ByteBuffer? {
        if(lastFrame == null){
            lastFrame = player.provide()
        }
        val data = lastFrame?.data
        lastFrame = null
        return ByteBuffer.wrap(data)
    }

    override fun canProvide(): Boolean {
        lastFrame = player.provide()
        return lastFrame != null
    }

    override fun isOpus() = true
}