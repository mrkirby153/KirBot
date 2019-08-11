package me.mrkirby153.KirBot.modules.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.getClearance
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.hooks.SubscribeEvent


class MusicModule : Module("music") {

    val playingGuilds = mutableSetOf<String>()

    private val managers = mutableMapOf<String, MusicManager>()

    override fun onLoad() {

    }

    @Periodic(5)
    fun updateQueue() {
        playingGuilds.mapNotNull { Bot.shardManager.getGuildById(it) }.map {
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

    @Subscribe
    fun onGuildLeave(event: GuildLeaveEvent) {
        managers.remove(event.guild.id)
        stopPlaying(event.guild)
    }

    @Subscribe
    fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        if (getCurrentChannel(event.guild) == event.channelJoined)
            getManager(event.guild).resume(true)
    }

    @Subscribe
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
            channel: GuildChannel) = channel.members.none { m -> m.user.id != channel.guild.selfMember.user.id }

    private fun getCurrentChannel(guild: Guild): VoiceChannel? {
        return guild.selfMember.voiceState!!.channel
    }

    companion object {
        fun isDJ(member: Member?): Boolean {
            if(member == null)
                return false
            if (member.user.getClearance(member.guild) > CLEARANCE_MOD)
                return true
            return member.roles.map { it.name }.firstOrNull { it.equals("DJ", true) } != null
        }

        fun alone(member: Member?): Boolean {
            if(member == null)
                return false
            if (!member.voiceState!!.inVoiceChannel())
                return false
            return member.voiceState!!.channel?.members?.filter { it != member.guild.selfMember }?.size == 1
        }
    }
}