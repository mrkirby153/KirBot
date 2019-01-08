package me.mrkirby153.KirBot.modules.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.module.Module
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.VoiceChannel
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent

class MusicModule : Module("music") {

    val playingGuilds = mutableSetOf<String>()

    private val managers = mutableMapOf<String, MusicManager>()

    override fun onLoad() {

    }

    @Periodic(5)
    fun updateQueue() {
        playingGuilds.mapNotNull { Bot.shardManager.getGuild(it) }.map {
            getManager(it)
        }.forEach {
            it.updateQueue()
            it.updateVoiceState()
        }
    }

    /**
     * Registers a guild as playing music
     */
    fun startPlaying(guild: Guild) {
        playingGuilds.add(guild.id)
    }

    /**
     * Unregisters a guild as playing music
     */
    fun stopPlaying(guild: Guild) {
        playingGuilds.remove(guild.id)
    }

    /**
     * Gets the [MusicManager] for the given guild
     *
     * @return The Music manager
     */
    fun getManager(guild: Guild): MusicManager {
        return managers.getOrPut(guild.id) {
            MusicManager(guild)
        }
    }

    @SubscribeEvent
    fun onGuildLeave(event: GuildLeaveEvent) {
        managers.remove(event.guild.id)
        stopPlaying(event.guild)
    }

    @SubscribeEvent
    fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (getCurrentChannel(event.guild) == event.channelJoined)
            getManager(event.guild).resume(true)
    }

    @SubscribeEvent
    fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        if (getCurrentChannel(event.guild) == event.channelLeft && isChannelEmpty(
                        event.channelLeft))
            getManager(event.guild).pause(true)
    }

    @SubscribeEvent
    fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        if (getCurrentChannel(event.guild) == event.channelJoined) {
            getManager(event.guild).resume(true)
        }
        if (getCurrentChannel(event.guild) == event.channelLeft && isChannelEmpty(
                        event.channelLeft))
            getManager(event.guild).pause(true)
    }

    private fun isChannelEmpty(
            channel: Channel) = channel.members.none { m -> m.user.id != channel.guild.selfMember.user.id }

    private fun getCurrentChannel(guild: Guild): VoiceChannel? {
        return guild.selfMember.voiceState.channel
    }
}