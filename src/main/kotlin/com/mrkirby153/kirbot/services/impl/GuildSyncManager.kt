package com.mrkirby153.kirbot.services.impl

import com.mrkirby153.kirbot.entity.DiscordUser
import com.mrkirby153.kirbot.entity.guild.Channel
import com.mrkirby153.kirbot.entity.guild.GuildEntity
import com.mrkirby153.kirbot.entity.guild.Role
import com.mrkirby153.kirbot.entity.guild.repo.ChannelRepository
import com.mrkirby153.kirbot.entity.guild.repo.GuildRepository
import com.mrkirby153.kirbot.entity.guild.repo.RoleRepository
import com.mrkirby153.kirbot.entity.repo.DiscordUserRepository
import com.mrkirby153.kirbot.events.AllShardsReadyEvent
import com.mrkirby153.kirbot.services.GuildSyncService
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateNameEvent
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.voice.update.GenericVoiceChannelUpdateEvent
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.events.role.RoleCreateEvent
import net.dv8tion.jda.api.events.role.RoleDeleteEvent
import net.dv8tion.jda.api.events.role.update.GenericRoleUpdateEvent
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent
import net.dv8tion.jda.api.events.role.update.RoleUpdatePermissionsEvent
import net.dv8tion.jda.api.events.role.update.RoleUpdatePositionEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.sharding.ShardManager
import org.apache.logging.log4j.LogManager
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture
import kotlin.system.measureTimeMillis

@Service
@Transactional
class GuildSyncManager(val channelRepository: ChannelRepository,
                       val roleRepository: RoleRepository,
                       val discordUserRepository: DiscordUserRepository,
                       val guildRepository: GuildRepository,
                       val shardManager: ShardManager) :
        GuildSyncService {

    private val log = LogManager.getLogger()


    @Async
    override fun sync(guild: Guild): CompletableFuture<Void> {
        log.debug("Performing sync of {}", guild)

        guildRepository.findByIdWithTrashed(guild.id).ifPresentOrElse({ guildEntity ->
            log.debug("Updating guild {}", guild)
            var changed = guildEntity.update(guild)
            try {
                val owner = guild.retrieveOwner().complete()
                if (guildEntity.owner != owner.id) {
                    changed = true
                    guildEntity.owner = owner.id
                }
            } catch (e: Throwable) {
                if (e is ErrorResponseException) {
                    when (e.errorResponse) {
                        ErrorResponse.UNKNOWN_USER, ErrorResponse.UNKNOWN_MEMBER -> {
                            changed = true
                            guildEntity.owner = null
                        }
                        else -> log.error("Unhandled error response received: {}", e.errorResponse)
                    }
                } else {
                    log.error("Could not update owner on {}", guild, e)
                }
            }
            if (guildEntity.deletedAt != null) {
                changed = true
                log.debug("Restoring trashed guild {}", guild)
                guildEntity.restore()
            }
            if (changed)
                guildRepository.saveAndFlush(guildEntity)
        }, {
            log.debug("Guild not found. Creating")
            guildRepository.saveAndFlush(GuildEntity(guild))
        })

        syncChannels(guild)
        syncRoles(guild)
        syncSeenUsers(guild)
        log.debug("Sync of {} succeeded", guild)
        return CompletableFuture.completedFuture(null)
    }

    @Async
    fun syncChannels(guild: Guild): CompletableFuture<Void> {
        log.debug("Syncing channels on {}", guild.id)
        val time = measureTimeMillis {
            val channelIds = guild.textChannels.union(guild.voiceChannels).map { it.id }.toList()
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
        val time = measureTimeMillis {
            val roleIds = guild.roles.map { it.id }.toList()
            val savedRoleIds = roleRepository.getAllByServerId(guild.id).mapNotNull { it.id }

            val newRoles = roleIds - savedRoleIds
            val deletedRoles = savedRoleIds - roleIds
            log.debug("New Roles: {}, Deleted Roles: {}", newRoles.size, deletedRoles.size)

            roleRepository.deleteAllByIdIn(deletedRoles)
            roleRepository.saveAll(newRoles.mapNotNull { guild.getRoleById(it) }.map { Role(it) })
            roleRepository.saveAll(roleRepository.findAllById(roleIds).mapNotNull { role ->
                role?.id ?: return@mapNotNull null
                val guildRole = guild.getRoleById(role.id) ?: return@mapNotNull null
                log.debug("Updating role {} [{}]", guildRole.name, guildRole.id)
                if (role.update(guildRole))
                    role
                else
                    null
            })
        }
        log.debug("Synced roles on {} in {}ms", guild.id, time)
        return CompletableFuture.completedFuture(null)
    }

    @Async
    fun syncSeenUsers(guild: Guild): CompletableFuture<Void> {
        log.debug("Syncing seen users on {}", guild.id)
        val cf = CompletableFuture<Void>()
        guild.loadMembers().onSuccess { members ->
            val memberEntities = mutableListOf<DiscordUser>()
            members.forEach { member ->
                // TODO: 8/9/20 Investigate how this affects performance retrieving everything individually versus in a batch
                discordUserRepository.findById(member.id).ifPresentOrElse({ existingMember ->
                    val changed = existingMember.username != member.user.name || existingMember.discriminator != member.user.discriminator.toInt()
                    existingMember.username = member.user.name
                    existingMember.discriminator = member.user.discriminator.toInt()
                    // We only want to save users that have changed
                    if (changed) {
                        memberEntities.add(existingMember)
                    }
                }, {
                    memberEntities.add(DiscordUser(member.user))
                })
            }
            log.debug("Saving {} users", memberEntities.size)
            discordUserRepository.saveAll(memberEntities)
            log.debug("Synced seen users on {}", guild.id)
            cf.complete(null)
        }.onError { t ->
            cf.completeExceptionally(t)
        }
        return cf
    }

    @EventListener
    fun onReady(event: AllShardsReadyEvent) {
        log.debug("All shards ready. Syncing guilds")
        shardManager.guilds.forEach { guild ->
            sync(guild)
        }
    }

    @EventListener
    fun onGuildJoin(event: GuildJoinEvent) {
        log.debug("Joined new guild. Performing sync")
        sync(event.guild)
    }

    @EventListener
    @Async
    fun onTextChannelCreate(event: TextChannelCreateEvent) {
        log.debug("Creating new text channel {} in {}", event.channel, event.guild)
        channelRepository.save(Channel(event.channel))
    }

    @EventListener
    @Async
    fun onTextChannelUpdate(event: TextChannelUpdateNameEvent) {
        log.debug("Updating text channel {} in {}", event.channel, event.guild)
        channelRepository.findById(event.channel.id).ifPresent {
            if (it.update(event.channel))
                channelRepository.save(it)
        }
    }

    @EventListener
    @Async
    fun onTextChannelDelete(event: TextChannelDeleteEvent) {
        log.debug("Deleting text channel {} in {}", event.channel, event.guild)
        channelRepository.deleteById(event.channel.id)
    }

    @EventListener
    @Async
    fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
        log.debug("Creating new voice channel {} in {}", event.channel, event.guild)
        channelRepository.save(Channel(event.channel))
    }

    @EventListener
    @Async
    fun onVoiceChannelUpdate(event: GenericVoiceChannelUpdateEvent<*>) {
        log.debug("Updating voice channel {} in {}", event.channel, event.guild)
        channelRepository.findById(event.channel.id).ifPresent {
            if (it.update(event.channel))
                channelRepository.save(it)
        }
    }

    @EventListener
    @Async
    fun onVoiceChannelDelete(event: VoiceChannelDeleteEvent) {
        log.debug("Deleting voice channel {} in {}", event.channel, event.guild)
        channelRepository.deleteById(event.channel.id)
    }

    @EventListener
    @Async
    fun onRoleCreate(event: RoleCreateEvent) {
        log.debug("Creating role {} in {}", event.role, event.guild)
        roleRepository.save(Role(event.role))
    }

    @EventListener
    @Async
    fun onRoleUpdate(event: GenericRoleUpdateEvent<*>) {
        if (event !is RoleUpdateNameEvent && event !is RoleUpdatePositionEvent && event !is RoleUpdatePermissionsEvent)
            return
        log.debug("Updating role {} in {}: {}", event.role, event.guild, event.javaClass)
        roleRepository.findById(event.role.id).ifPresent {
            if (it.update(event.role))
                roleRepository.save(it)
        }
    }

    @EventListener
    @Async
    fun onRoleDelete(event: RoleDeleteEvent) {
        log.debug("Deleting role {} in {}", event.role, event.guild)
        roleRepository.deleteById(event.role.id)
    }

    @EventListener
    @Async
    fun onMemberUpdateName(event: UserUpdateNameEvent) {
        log.debug("Updating user {}", event.user)
        discordUserRepository.findById(event.user.id).ifPresent {
            val changed = event.user.name != it.username || event.user.discriminator.toInt() != it.discriminator
            it.username = event.user.name
            it.discriminator = event.user.discriminator.toInt()
            if (changed)
                discordUserRepository.save(it)
        }
    }
}
