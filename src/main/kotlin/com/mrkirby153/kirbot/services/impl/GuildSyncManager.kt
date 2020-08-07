package com.mrkirby153.kirbot.services.impl

import com.mrkirby153.kirbot.entity.guild.Channel
import com.mrkirby153.kirbot.entity.guild.repo.ChannelRepository
import com.mrkirby153.kirbot.services.GuildSyncService
import net.dv8tion.jda.api.entities.Guild
import org.apache.logging.log4j.LogManager
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture
import kotlin.system.measureTimeMillis

@Service
@Transactional
class GuildSyncManager(val channelRepository: ChannelRepository) : GuildSyncService {

    private val log = LogManager.getLogger()


    @Async
    override fun sync(guild: Guild): CompletableFuture<Void> {
        syncChannels(guild)
        syncRoles(guild)
        syncSeenUsers(guild)
        return CompletableFuture.completedFuture(null)
    }

    @Async
    fun syncChannels(guild: Guild): CompletableFuture<Void> {
        log.debug("Syncing channels on {}", guild.id)
        val time = measureTimeMillis {
            val channelIds = guild.channels.map { it.id }.toList()
            val savedChannelIds = channelRepository.getAllByServerId(guild.id).mapNotNull { it.id }

            val newChannels = channelIds - savedChannelIds
            val deletedChannels = savedChannelIds - channelIds
            log.debug("New Channels: {}, Deleted Channels: {}", newChannels.size,
                    deletedChannels.size)

            channelRepository.deleteAllByIdIn(deletedChannels)
            channelRepository.saveAll(
                    newChannels.mapNotNull { guild.getGuildChannelById(it) }.map { Channel(it) })
            channelRepository.saveAll(channelRepository.findAllById(channelIds).mapNotNull { chan ->
                if (chan.id == null)
                    return@mapNotNull null
                val guildChan = guild.getGuildChannelById(chan.id) ?: return@mapNotNull null
                log.debug("Updating channel {} [{}]", guildChan.name, guildChan.id)
                if (chan.update(guildChan))
                    chan
                else
                    null
            })
        }
        log.debug("Synced channels on {} in {}ms", guild.id, time)
        return CompletableFuture.completedFuture(null)
    }

    @Async
    fun syncRoles(guild: Guild): CompletableFuture<Void> {
        log.debug("Syncing roles on {}", guild.id)
        log.debug("Synced roles on {}", guild.id)
        return CompletableFuture.completedFuture(null)
    }

    @Async
    fun syncSeenUsers(guild: Guild): CompletableFuture<Void> {
        log.debug("Syncing seen users on {}", guild.id)
        log.debug("Synced seen users on {}", guild.id)
        return CompletableFuture.completedFuture(null)
    }
}
