package com.mrkirby153.kirbot.services.impl

import com.mrkirby153.kirbot.entity.Infraction
import com.mrkirby153.kirbot.entity.repo.InfractionRepository
import com.mrkirby153.kirbot.events.AllShardsReadyEvent
import com.mrkirby153.kirbot.events.InfractionEvent
import com.mrkirby153.kirbot.events.UserBanEvent
import com.mrkirby153.kirbot.events.UserKickEvent
import com.mrkirby153.kirbot.events.UserMuteEvent
import com.mrkirby153.kirbot.events.UserTempBanEvent
import com.mrkirby153.kirbot.events.UserUnbanEvent
import com.mrkirby153.kirbot.events.UserUnmuteEvent
import com.mrkirby153.kirbot.events.UserWarnEvent
import com.mrkirby153.kirbot.services.InfractionService
import com.mrkirby153.kirbot.services.setting.GuildSettings
import com.mrkirby153.kirbot.services.setting.SettingsService
import com.mrkirby153.kirbot.utils.checkPermission
import com.mrkirby153.kirbot.utils.getMember
import com.mrkirby153.kirbot.utils.nameAndDiscrim
import com.mrkirby153.kirbot.utils.responseBuilder
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.exceptions.PermissionException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.sharding.ShardManager
import org.apache.logging.log4j.LogManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@Service
class InfractionManager(private val infractionRepository: InfractionRepository,
                        private val applicationEventPublisher: ApplicationEventPublisher,
                        private val settingsService: SettingsService,
                        private val shardManager: ShardManager) :
        InfractionService {

    private var nextInfractionRunsAt: Long? = null
    private var expireTaskRunning = false

    private val log = LogManager.getLogger()

    private fun checkPermission(context: InfractionService.InfractionContext,
                                vararg permissions: Permission) {
        permissions.forEach {
            if (!context.guild.checkPermission(it))
                throw PermissionException("Missing permission $it in ${context.guild.id}")
        }
    }

    private fun createInfraction(context: InfractionService.InfractionContext,
                                 type: Infraction.InfractionType,
                                 save: Boolean = true, expiresAt: Long? = null): Infraction {
        val entity = Infraction(context.userId, context.guild.id, type,
                context.reason ?: "No reason specified",
                expiresAt = if (expiresAt != null) Timestamp(expiresAt) else null)
        return if (save) {
            infractionRepository.save(entity)
        } else {
            entity
        }
    }

    override fun kick(
            infractionContext: InfractionService.InfractionContext): CompletableFuture<InfractionService.InfractionResult> {
        checkPermission(infractionContext, Permission.KICK_MEMBERS)

        infractionContext.user?.getMember(infractionContext.guild)
                ?: return CompletableFuture.failedFuture(
                        IllegalArgumentException("Member not found"))

        val infraction = createInfraction(infractionContext, Infraction.InfractionType.KICK)
        val dmFuture = dmUser(infractionContext.user, infractionContext.guild,
                infractionContext.issuer, infraction)

        applicationEventPublisher.publishEvent(
                UserKickEvent(
                        infraction, infractionContext.user, infractionContext.guild))

        return dmFuture.thenCompose {
            infractionContext.guild.kick(infractionContext.user.id,
                    infractionContext.reason).submit()
        }.thenApply {
            InfractionService.InfractionResult(dmFuture.get(), infraction)
        }
    }

    override fun ban(
            infractionContext: InfractionService.InfractionContext): CompletableFuture<InfractionService.InfractionResult> {
        checkPermission(infractionContext, Permission.BAN_MEMBERS)

        val infraction = createInfraction(infractionContext, Infraction.InfractionType.BAN)
        val dmFuture = if (infractionContext.user != null) dmUser(infractionContext.user,
                infractionContext.guild,
                infractionContext.issuer, infraction) else CompletableFuture.completedFuture(
                InfractionService.DmResult.NOT_SENT)

        applicationEventPublisher.publishEvent(
                UserBanEvent(infraction, infraction.userId, infractionContext.user,
                        infractionContext.guild))
        val banFuture = infractionContext.guild.ban(infractionContext.userId, 0,
                infractionContext.reason).submit()
        return dmFuture.thenCompose { banFuture }.thenApply {
            InfractionService.InfractionResult(dmFuture.get(), infraction)
        }
    }

    override fun tempBan(infractionContext: InfractionService.InfractionContext, duration: Long,
                         units: TimeUnit): CompletableFuture<InfractionService.InfractionResult> {
        checkPermission(infractionContext, Permission.BAN_MEMBERS)

        val durationMs = TimeUnit.MILLISECONDS.convert(duration, units)
        val expiresAt = System.currentTimeMillis() + durationMs
        val infraction = createInfraction(infractionContext, Infraction.InfractionType.TEMP_BAN,
                expiresAt = expiresAt)
        val dmFuture = if (infractionContext.user != null) dmUser(infractionContext.user,
                infractionContext.guild, infractionContext.issuer,
                infraction) else CompletableFuture.completedFuture(
                InfractionService.DmResult.NOT_SENT)

        applicationEventPublisher.publishEvent(
                UserTempBanEvent(infraction, infractionContext.userId, infractionContext.user,
                        infractionContext.guild,
                        expiresAt))
        val banFuture = infractionContext.guild.ban(infractionContext.userId, 0,
                infractionContext.reason).submit()
        return dmFuture.thenCompose { banFuture }.thenApply {
            InfractionService.InfractionResult(dmFuture.get(), infraction)
        }
    }

    override fun mute(
            infractionContext: InfractionService.InfractionContext): CompletableFuture<InfractionService.InfractionResult> {
        checkPermission(infractionContext, Permission.MANAGE_ROLES)

        infractionContext.user?.getMember(infractionContext.guild)
                ?: return CompletableFuture.failedFuture(
                        IllegalArgumentException("Member not found"))

        val role = getMutedRole(infractionContext.guild) ?: return CompletableFuture.failedFuture(
                IllegalStateException("Muted role not found"))

        val infraction = createInfraction(infractionContext, Infraction.InfractionType.MUTE)
        val dmFuture = dmUser(infractionContext.user, infractionContext.guild,
                infractionContext.issuer, infraction)

        applicationEventPublisher.publishEvent(
                UserMuteEvent(infraction, infractionContext.user, infractionContext.guild))

        return infractionContext.guild.addRoleToMember(infractionContext.user.id,
                role).submit().thenCompose { dmFuture }.thenApply {
            InfractionService.InfractionResult(it, infraction)
        }
    }

    override fun unmute(
            infractionContext: InfractionService.InfractionContext): CompletableFuture<InfractionService.InfractionResult> {
        checkPermission(infractionContext, Permission.MANAGE_ROLES)

        infractionContext.user?.getMember(infractionContext.guild)
                ?: return CompletableFuture.failedFuture(
                        IllegalArgumentException("Member not found"))

        val role = getMutedRole(infractionContext.guild) ?: return CompletableFuture.failedFuture(
                IllegalStateException("Muted role not found"))


        infractionRepository.getAllActiveInfractionsByType(
                infractionContext.user.id, infractionContext.guild.id,
                Infraction.InfractionType.MUTE).forEach { inf ->
            inf.active = false
            infractionRepository.save(inf)
        }

        applicationEventPublisher.publishEvent(
                UserUnmuteEvent(infractionContext.user, infractionContext.guild))

        return infractionContext.guild.removeRoleFromMember(infractionContext.user.id,
                role).submit().thenApply {
            InfractionService.InfractionResult(InfractionService.DmResult.NOT_SENT, null)
        }
    }

    override fun tempMute(infractionContext: InfractionService.InfractionContext, duration: Long,
                          units: TimeUnit): CompletableFuture<InfractionService.InfractionResult> {
        checkPermission(infractionContext, Permission.MANAGE_ROLES)

        infractionContext.user?.getMember(infractionContext.guild)
                ?: return CompletableFuture.failedFuture(
                        IllegalArgumentException("Member not found"))

        val role = getMutedRole(infractionContext.guild) ?: return CompletableFuture.failedFuture(
                IllegalStateException("Muted role not found"))

        val infraction = createInfraction(infractionContext, Infraction.InfractionType.TEMP_MUTE,
                expiresAt = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(duration,
                        units))
        val dmFuture = dmUser(infractionContext.user, infractionContext.guild,
                infractionContext.issuer, infraction)

        applicationEventPublisher.publishEvent(
                UserMuteEvent(infraction, infractionContext.user, infractionContext.guild))

        return infractionContext.guild.addRoleToMember(infractionContext.user.id,
                role).submit().thenCompose { dmFuture }.thenApply {
            InfractionService.InfractionResult(it, infraction)
        }
    }

    override fun warn(
            infractionContext: InfractionService.InfractionContext): CompletableFuture<InfractionService.InfractionResult> {
        val infraction = createInfraction(infractionContext, Infraction.InfractionType.WARN)

        val future = if (infractionContext.user != null) dmUser(infractionContext.user,
                infractionContext.guild,
                infractionContext.issuer, infraction) else CompletableFuture.completedFuture(
                InfractionService.DmResult.NOT_SENT)
        applicationEventPublisher.publishEvent(
                UserWarnEvent(infraction, infractionContext.userId, infractionContext.user,
                        infractionContext.guild))
        return future.thenApply {
            InfractionService.InfractionResult(it, infraction)
        }
    }

    override fun unban(guild: Guild, user: String, issuer: User,
                       reason: String): CompletableFuture<InfractionService.InfractionResult> {
        var infraction = Infraction(user, guild.id, Infraction.InfractionType.UNBAN, reason)
        infraction = infractionRepository.save(infraction)

        applicationEventPublisher.publishEvent(UserUnbanEvent(infraction, user, guild))
        return guild.unban(infraction.userId).reason(infraction.reason).submit().thenApply {
            InfractionService.InfractionResult(InfractionService.DmResult.NOT_SENT, infraction)
        }
    }

    override fun getInfractions(user: User,
                                guild: Guild) = infractionRepository.getAllByUserIdAndGuild(user.id,
            guild.id)

    /**
     * Retrieves the muted role on a guild
     *
     * @param guild The guild to get the muted role on
     * @return The muted role if it exists
     */
    fun getMutedRole(guild: Guild): Role? {
        val roleId = settingsService.getSetting(GuildSettings.mutedRole, guild) ?: return null
        return guild.getRoleById(roleId)
    }

    /**
     * Optionally sends a DM to the user about their infraction
     *
     * @param user The user to DM
     * @param guild The guild to reference in the DM message
     * @param issuer The user issuing the infraction
     * @param infraction The infraction
     * @return A CompletableFuture with the DM result. If the infraction does not start with `[ADM]`
     * or `[DM]` this will return immediately with [InfractionService.DmResult.NOT_SENT]
     */
    fun dmUser(user: User, guild: Guild, issuer: User,
               infraction: Infraction): CompletableFuture<InfractionService.DmResult> {
        if (!infraction.reason.startsWith("[DM]") && !infraction.reason.startsWith("[ADM]"))
            return CompletableFuture.completedFuture(InfractionService.DmResult.NOT_SENT)
        val anonymous = infraction.reason.startsWith("[ADM]")
        val reason = infraction.reason.replace(Regex("\\[A?DM]"), "").trim()
        val future = CompletableFuture<InfractionService.DmResult>()
        try {
            user.openPrivateChannel().queue { channel ->
                val duration = if (infraction.expiresAt != null) Time.format(1,
                        System.currentTimeMillis() - infraction.expiresAt!!.toInstant()!!.toEpochMilli()) else "Forever"
                channel.responseBuilder.sendMessage(buildString {
                    append("You have been ")
                    append(when (infraction.type) {
                        Infraction.InfractionType.MUTE -> "muted"
                        Infraction.InfractionType.TEMP_MUTE -> "temporarily muted for $duration"
                        Infraction.InfractionType.KICK -> "kicked"
                        Infraction.InfractionType.BAN -> "banned"
                        Infraction.InfractionType.UNBAN -> "unbanned"
                        Infraction.InfractionType.TEMP_BAN -> "temporarily banned for $duration"
                        Infraction.InfractionType.WARN -> "warned"
                    })
                    append(when (infraction.type) {
                        Infraction.InfractionType.WARN, Infraction.InfractionType.MUTE, Infraction.InfractionType.TEMP_MUTE -> " in"
                        else -> " from"
                    })
                    append(" {{**${guild.name}**}}")
                    if (!anonymous)
                        append(" by ${issuer.nameAndDiscrim}")
                    append(": {{`$reason`}}")
                })?.queue { future.complete(InfractionService.DmResult.SENT) }
            }
        } catch (e: ErrorResponseException) {
            if (e.errorResponse == ErrorResponse.CANNOT_SEND_TO_USER)
                return CompletableFuture.completedFuture(InfractionService.DmResult.SEND_ERROR)
        }
        return future
    }

    @EventListener
    fun onReady(event: AllShardsReadyEvent) {
        log.info("Shards are ready. Waiting for next infraction")
        try {
            expireTaskRunning = true
            runExpiredInfractions()
        } finally {
            expireTaskRunning = false
        }
        waitForNextInfraction()
    }

    @EventListener
    fun onInfraction(event: InfractionEvent) {
        log.debug("New infraction. Rescheduling infractions")
        waitForNextInfraction()
    }

    @Scheduled(fixedRate = 1000L)
    fun runExpiredTask() {
        if (expireTaskRunning) {
            return
        }
        try {
            expireTaskRunning = true
            val runAt = nextInfractionRunsAt ?: return
            if (System.currentTimeMillis() < runAt)
                return
            runExpiredInfractions()
        } finally {
            expireTaskRunning = false
        }
        waitForNextInfraction()
    }

    fun waitForNextInfraction() {
        if (expireTaskRunning) {
            log.debug("Expire task is running")
            return
        }
        infractionRepository.getNextInfractionToExpire().ifPresentOrElse({
            log.debug("next infraction runs at ${it.expiresAt}")
            nextInfractionRunsAt = it.expiresAt?.time
        }, {
            nextInfractionRunsAt = null
        })
    }

    /**
     * Runs all expired infractions
     */
    fun runExpiredInfractions() {
        log.debug("Running expired infractions")
        infractionRepository.getAllInfractionsExpiringBefore(
                Timestamp.from(Instant.now())).forEach {
            log.debug("Expiring infraction {}", it.id)
            onInfractionExpire(it)
        }
    }

    /**
     * Handles the [infraction] when it expires
     */
    private fun onInfractionExpire(infraction: Infraction) {
        log.debug("Processing expiration of ${infraction.id} (${infraction.type})")
        try {
            val guild = shardManager.getGuildById(infraction.guild) ?: return
            val user = shardManager.getUserById(infraction.userId)
            when (infraction.type) {
                Infraction.InfractionType.TEMP_MUTE -> {
                    if (user != null) {
                        unmute(InfractionService.InfractionContext(user, guild, guild.jda.selfUser,
                                "Timed mute expired"))
                    } else {
                        log.debug("User $user is no longer a member of $guild")
                    }
                }
                Infraction.InfractionType.TEMP_BAN -> {
                    unban(guild, infraction.userId, guild.jda.selfUser, "Timed ban expired")
                }
                else -> {
                    // Not a temp infraction, do nothing
                }
            }
        } finally {
            infraction.active = false
            infractionRepository.save(infraction)
        }
    }
}