package me.mrkirby153.KirBot.infraction

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.query.elements.OrderElement
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.DiscordUser
import me.mrkirby153.KirBot.inject.Injectable
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.InfractionModule
import me.mrkirby153.KirBot.modules.Logger
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.utils.canAssign
import me.mrkirby153.KirBot.utils.checkPermission
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.KirBot.utils.settings.GuildSettings
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.GuildBanEvent
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.requests.ErrorResponse
import net.dv8tion.jda.api.sharding.ShardManager
import java.sql.Timestamp
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Injectable
class Infractions @Inject constructor(private val logger: Logger, private val shardManager: ShardManager) {

    private var nextTask: ScheduledFuture<*>? = null
    private var nextInfractionId: Long? = null
    private val scheduler = Executors.newSingleThreadScheduledExecutor(
            ThreadFactoryBuilder().setNameFormat("Infraction-Scheduler").setDaemon(true).build())


    fun waitForInfraction() {
        Bot.LOG.debug("Queueing Infractions")
        val nextInfraction = Model.query(Infraction::class.java).whereNotNull("expires_at").where(
                "active", true).orderBy("expires_at", OrderElement.Direction.ASC).limit(1).first()
        if (nextInfraction == null) {
            Bot.LOG.debug("No infractions left to wait for")
            return
        }
        if (nextInfraction.id == nextInfractionId) {
            Bot.LOG.debug("Not re-scheduling the next infraction as it's the same")
        } else {
            if (nextTask != null) {
                Bot.LOG.debug("Re-scheduling $nextInfractionId -- ${nextInfraction.id} runs sooner")
                nextTask?.cancel(true)
                nextTask = null
                nextInfractionId = null
            }
        }

        val runIn = nextInfraction.expiresAt!!.time - System.currentTimeMillis()
        Bot.LOG.debug("Infraction expiring in ${Time.formatLong(runIn)}")
        nextInfractionId = nextInfraction.id
        nextTask = scheduler.schedule({
            runExpiredInfractions()
            nextTask = null
            nextInfractionId = null
            waitForInfraction()
        }, runIn, TimeUnit.MILLISECONDS)
    }

    private fun runExpiredInfractions() {
        val expired = Model.query(Infraction::class.java).where("active", true).whereNotNull(
                "expires_at").where("expires_at", "<", Timestamp.from(Instant.now())).get()
        Bot.LOG.debug("Running ${expired.count()} expired infractions")
        expired.forEach {
            Bot.LOG.debug("Expiring ${it.id}")
            onInfractionExpire(it)
        }
    }


    private fun onInfractionExpire(infraction: Infraction) {
        val guild = shardManager.getGuildById(infraction.guild)
        val user = shardManager.getUserById(infraction.userId)
        if (guild == null ) {
            // Guild was not found. Revoke and do nothing
            infraction.revoke()
            return
        }
        try {
            when (infraction.type) {
                InfractionType.TEMPMUTE -> {
                    if(user != null) {
                        if (guild.getMember(user) == null) {
                            Bot.LOG.debug(
                                    "User $user is no longer a member of $guild. Skipping infraction")
                            return
                        }
                        unmute(user.id, guild, guild.jda.selfUser.id, "Timed mute expired")
                    }
                }
                InfractionType.TEMPBAN -> {
                    Bot.applicationContext.get(InfractionModule::class.java).ignoreUnbans.add(infraction.userId)
                    guild.unban(infraction.userId).queue()
                    guild.kirbotGuild.logManager.genericLog(LogEvent.USER_UNBAN, ":rotating_light:",
                            buildString {
                                append(lookupUser(infraction.userId, true))
                                append(" unbanned by **${lookupUser(guild.selfMember.user.id)}**")
                                append(" (`Timed ban expired`)")
                            })
                }
                InfractionType.TEMPROLE -> {
                    // Remove the role from the user
                    val roleId = infraction.metadata
                    if (roleId != null && user != null) {
                        val role = guild.getRoleById(roleId)
                        val member = guild.getMember(user)
                        if (role != null && member != null && role in member.roles) {
                            if (guild.checkPermission(
                                            Permission.MANAGE_ROLES) && guild.selfMember.canAssign(
                                            role)) {
                                logger.debouncer.create(
                                        GuildMemberRoleRemoveEvent::class.java,
                                        Pair("user", infraction.userId), Pair("role", role.id))
                                guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_REMOVE,
                                        ":key:",
                                        "Removed **${role.name}** from ${user.logName}: `Temprole expired`")
                                guild.removeRoleFromMember(member, role).reason(
                                        "#${infraction.id} - Temprole expired").queue()
                            }
                        }
                    }
                }
                else -> {
                    // Not a temporary infraction, don't do anything
                }
            }
        } catch (e: java.lang.Exception) {
            Bot.LOG.warn("An exception occurred when expiring infraction {}", infraction.id, e)
        } finally {
            // Revoke the infraction
            infraction.revoke()
        }
    }

    fun kick(user: String, guild: Guild, issuer: String,
             reason: String? = null): CompletableFuture<InfractionResult> {
        if (!guild.selfMember.hasPermission(Permission.KICK_MEMBERS)) {
            return CompletableFuture.completedFuture(
                    InfractionResult(false, Infractions.DmResult.NOT_SENT,
                            "Missing `KICK_MEMBERS`"))
        }
        val future = CompletableFuture<InfractionResult>()

        logger.debouncer.create(GuildMemberRemoveEvent::class.java,
                Pair("user", user), Pair("guild", guild.id))

        val inf = createInfraction(user, guild, if (issuer == "1") user else issuer, reason,
                InfractionType.KICK)
        val result = dmUser(user, guild, inf).get()
        try {
            Bot.applicationContext.get(InfractionModule::class.java).debouncer.create(
                    GuildMemberRemoveEvent::class.java, Pair("user", user), Pair("guild", guild.id));
            guild.kick(guild.getMemberById(user) ?: return CompletableFuture.completedFuture(
                    InfractionResult(false, result, "Member not found")), reason ?: "").queue {
                future.complete(InfractionResult(true, result))
            }
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_KICK, ":boot:", buildString {
            append(lookupUser(user, true))
            append(" kicked by **${lookupUser(issuer)}**")
            if (reason != null) {
                append(": `$reason`")
            }
        })
        return future
    }

    fun warn(user: String, guild: Guild, issuer: String,
             reason: String? = null): CompletableFuture<InfractionResult> {
        val inf = createInfraction(user, guild, issuer, reason, InfractionType.WARN)
        val r = dmUser(user, guild, inf).get()
        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_WARN, ":warning:", buildString {
            append(lookupUser(user, true))
            append(" was warned by **${lookupUser(issuer)}**")
            if (reason != null) {
                append(": `$reason`")
            }
        })
        return CompletableFuture.completedFuture(InfractionResult(true, r))
    }

    fun ban(user: String, guild: Guild, issuer: String, reason: String? = null,
            purgeDays: Int = 0): CompletableFuture<InfractionResult> {
        if (!guild.selfMember.hasPermission(Permission.BAN_MEMBERS))
            return CompletableFuture.completedFuture(
                    InfractionResult(false, Infractions.DmResult.UNKNOWN,
                            "Missing `BAN_MEMBEBRS` permission"))

        Bot.applicationContext.get(InfractionModule::class.java).debouncer.create(GuildBanEvent::class.java,
                Pair("user", user), Pair("guild", guild.id))
        logger.debouncer.create(GuildMemberRemoveEvent::class.java,
                Pair("user", user), Pair("guild", guild.id))

        val future = CompletableFuture<InfractionResult>()
        val inf = createInfraction(user, guild, if (issuer == "1") user else issuer, reason,
                InfractionType.BAN)
        val r = dmUser(user, guild, inf).get()
        try {
            guild.ban(user, purgeDays, reason).queue {
                future.complete(InfractionResult(true, r))
            }
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_BAN, ":rotating_light:", buildString {
            append(lookupUser(user, true))
            append(" banned by **${lookupUser(issuer)}**")
            if (reason != null)
                append(": `$reason`")
        })
        return future
    }

    fun softban(user: String, guild: Guild, issuer: String,
                reason: String? = null): CompletableFuture<InfractionResult> {
        if (!guild.selfMember.hasPermission(Permission.BAN_MEMBERS))
            return CompletableFuture.completedFuture(
                    InfractionResult(false, Infractions.DmResult.UNKNOWN, "Missing BAN_MEMBERS"))

        Bot.applicationContext.get(InfractionModule::class.java).debouncer.create(GuildBanEvent::class.java,
                Pair("user", user), Pair("guild", guild.id))
        Bot.applicationContext.get(InfractionModule::class.java).debouncer.create(GuildUnbanEvent::class.java,
                Pair("user", user), Pair("guild", guild.id))
        logger.debouncer.create(GuildMemberRemoveEvent::class.java,
                Pair("user", user), Pair("guild", guild.id))

        val future = CompletableFuture<InfractionResult>()
        val inf = createInfraction(user, guild, if (issuer == "1") user else issuer, reason,
                InfractionType.KICK)
        val r = dmUser(user, guild, inf).get()

        try {
            guild.ban(user, 7, reason).queue {
                guild.unban(user).queue {
                    future.complete(InfractionResult(true, r))
                }
            }
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }
        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_KICK, ":boot:", buildString {
            append(lookupUser(user, true))
            append(" soft-banned by **${lookupUser(issuer)}**")
            if (reason != null)
                append(": `$reason`")
        })
        return future
    }

    fun unban(user: String, guild: Guild, issuer: String,
              reason: String = ""): CompletableFuture<InfractionResult> {
        if (!guild.selfMember.hasPermission(Permission.BAN_MEMBERS))
            return CompletableFuture.completedFuture(
                    InfractionResult(false, Infractions.DmResult.UNKNOWN, "Missing BAN_MEMBERS"))
        Bot.applicationContext.get(InfractionModule::class.java).debouncer.create(GuildUnbanEvent::class.java,
                Pair("user", user), Pair("guild", guild.id))
        val future = CompletableFuture<InfractionResult>()
        try {
            guild.retrieveBanList().queue { banList ->
                if (user in banList.map { it.user.id })
                    guild.unban(user).queue {
                        future.complete(InfractionResult(true, Infractions.DmResult.NOT_SENT))
                    }
                else {
                    Bot.LOG.debug("$user is not banned so not unbanning")
                    future.complete(InfractionResult(true, Infractions.DmResult.NOT_SENT))
                }
            }
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }

        // Deactivate all the users active bans (should only be one)
        getActiveInfractions(user,
                guild).filter { it.type == InfractionType.BAN || it.type == InfractionType.TEMPBAN }.forEach { ban ->
            ban.revoke()
        }
        createInfraction(user, guild, if (issuer == "1") user else issuer, reason,
                InfractionType.UNBAN)
        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_UNBAN, ":rotating_light:",
                buildString {
                    append(lookupUser(user, true))
                    append(" unbanned by **${lookupUser(issuer)}**")
                    if (reason.isNotBlank())
                        append("(`$reason`)")
                })
        return future
    }

    fun mute(user: String, guild: Guild, issuer: String, reason: String? = null,
             createInfraction: Boolean = true): CompletableFuture<InfractionResult> {
        if (!guild.selfMember.hasPermission(Permission.MANAGE_ROLES))
            return CompletableFuture.completedFuture(
                    InfractionResult(false, DmResult.UNKNOWN, "Missing MANAGE_ROLES"))
        val member = guild.getMemberById(user) ?: return CompletableFuture.completedFuture(
                InfractionResult(false, DmResult.UNKNOWN, "User not found"))
        val future = CompletableFuture<InfractionResult>()
        addMutedRole(member.user, guild,
                "Mute by ${lookupUser(issuer)}: ${reason ?: ""}").thenAccept {
            if (!it) {
                future.complete(InfractionResult(false, Infractions.DmResult.NOT_SENT,
                        "Could not add muted role"))
                return@thenAccept
            }
            var dmResult = DmResult.NOT_SENT
            if (createInfraction) {
                val inf = createInfraction(user, guild, if (issuer == "1") user else issuer, reason,
                        InfractionType.MUTE)
                dmResult = dmUser(user, guild, inf).get()
            }

            future.complete(InfractionResult(true, dmResult))

            guild.kirbotGuild.logManager.genericLog(LogEvent.USER_MUTE, ":zipper_mouth:",
                    buildString {
                        append(lookupUser(user, true))
                        append(" muted by **${lookupUser(issuer)}**")
                        if (reason != null) {
                            append(": `$reason`")
                        }
                    })
        }
        return future
    }

    fun unmute(user: String, guild: Guild, issuer: String,
               reason: String? = null): CompletableFuture<InfractionResult> {
        if (!guild.selfMember.hasPermission(Permission.MANAGE_ROLES))
            return CompletableFuture.completedFuture(
                    InfractionResult(false, Infractions.DmResult.UNKNOWN, "Missing MANAGE_ROLES"))

        val future = CompletableFuture<InfractionResult>()
        removeMutedRole(guild.getMemberById(user)!!.user, guild,
                "Unmute by ${lookupUser(issuer)}: ${reason ?: ""}").thenAccept { result ->
            if (!result) {
                future.complete(InfractionResult(false, Infractions.DmResult.NOT_SENT,
                        "Could not remove muted role"))
                return@thenAccept
            }
            getActiveInfractions(user,
                    guild).filter { it.type == InfractionType.MUTE || it.type == InfractionType.TEMPMUTE }.forEach { mute ->
                mute.revoke()
            }

            future.complete(InfractionResult(true, Infractions.DmResult.NOT_SENT))

            guild.kirbotGuild.logManager.genericLog(LogEvent.USER_UNMUTE, ":open_mouth:",
                    buildString {
                        append(lookupUser(user, true))
                        append(" unmuted by **${lookupUser(issuer)}**")
                        if (reason != null) {
                            append(": `$reason`")
                        }
                    })
        }
        return future
    }

    fun tempMute(user: String, guild: Guild, issuer: String, duration: Long, units: TimeUnit,
                 reason: String? = null): CompletableFuture<InfractionResult> {
        if (!guild.selfMember.hasPermission(Permission.MANAGE_ROLES))
            return CompletableFuture.completedFuture(
                    InfractionResult(false, Infractions.DmResult.UNKNOWN, "Missing MANAGE_ROLES"))

        val future = CompletableFuture<InfractionResult>()
        addMutedRole(guild.getMemberById(user)!!.user, guild,
                "Temp mute by ${lookupUser(issuer)}: ${reason ?: ""}").thenAccept { success ->
            if (!success) {
                future.complete(InfractionResult(false, Infractions.DmResult.NOT_SENT,
                        "Could not add muted role"))
                return@thenAccept
            }
            val inf = createInfraction(user, guild, if (issuer == "1") user else issuer, reason,
                    InfractionType.TEMPMUTE, Timestamp.from(
                    Instant.now().plusMillis(TimeUnit.MILLISECONDS.convert(duration, units))))
            val r = dmUser(user, guild, inf).get()
            future.complete(InfractionResult(true, r))
            waitForInfraction()

            guild.kirbotGuild.logManager.genericLog(LogEvent.USER_MUTE, ":zipper_mouth:",
                    buildString {
                        append(lookupUser(user, true))
                        append(" temp muted by **${lookupUser(issuer)}** for ${Time.formatLong(
                                TimeUnit.MILLISECONDS.convert(duration, units),
                                Time.TimeUnit.SECONDS)}")
                        if (reason != null) {
                            append(": `$reason`")
                        }
                    })
        }

        return future
    }

    fun tempban(user: String, guild: Guild, issuer: String, duration: Long, units: TimeUnit,
                reason: String? = null, purgeDays: Int = 0): CompletableFuture<InfractionResult> {
        if (!guild.selfMember.hasPermission(Permission.BAN_MEMBERS))
            return CompletableFuture.completedFuture(
                    InfractionResult(false, Infractions.DmResult.UNKNOWN, "Missing BAN_MEMBERS"))

        Bot.applicationContext.get(InfractionModule::class.java).ignoreBans.add(user)
        val inf = createInfraction(user, guild, issuer, reason, InfractionType.TEMPBAN,
                Timestamp.from(
                        Instant.now().plusMillis(TimeUnit.MILLISECONDS.convert(duration, units))))

        val r = dmUser(user, guild, inf).get()
        val future = CompletableFuture<InfractionResult>()
        guild.retrieveBanList().queue { banList ->
            if (user !in banList.map { it.user.id })
                try {
                    guild.ban(user, purgeDays, reason).queue {
                        future.complete(InfractionResult(true, r))
                    }
                } catch (e: Exception) {
                    future.completeExceptionally(e)
                }
        }
        waitForInfraction()
        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_BAN, ":rotating_light:", buildString {
            append(lookupUser(user, true))
            append(" temp-banned for ${Time.formatLong(
                    TimeUnit.MILLISECONDS.convert(duration, units))} by **${lookupUser(issuer)}**")
            if (reason != null)
                append(": `$reason`")
        })
        return future
    }

    fun createInfraction(user: String, guild: Guild, issuer: String, reason: String?,
                         type: InfractionType, expiresAt: Timestamp? = null,
                         metadata: String? = null): Infraction {
        val infraction = Infraction()
        infraction.userId = user
        infraction.guild = guild.id
        infraction.issuerId = issuer
        infraction.reason = reason
        infraction.type = type
        infraction.createdAt = Timestamp(System.currentTimeMillis())
        infraction.expiresAt = expiresAt
        infraction.metadata = metadata
        infraction.save()
        waitForInfraction()
        return infraction
    }

    fun getActiveInfractions(user: String, guild: Guild? = null): List<Infraction> {
        val qb = Model.where(Infraction::class.java, "user_id", user).where("active", true)
        if (guild != null) {
            qb.where("guild", guild.id)
        }
        return qb.get()
    }

    fun getAllInfractions(user: User, guild: Guild? = null): List<Infraction> {
        val qb = Model.where(Infraction::class.java, "user_id", user.id)
        if (guild != null)
            qb.where("guild", guild.id)
        return qb.get()
    }


    fun importFromBanlist(guild: KirBotGuild) {
        if (guild.selfMember.hasPermission(Permission.BAN_MEMBERS)) {
            guild.retrieveBanList().queue { bans ->
                val bannedIds = bans.map { it.user.id }
                if (bannedIds.isEmpty()) {
                    Bot.LOG.debug("banlist is empty")
                    return@queue
                }
                val recordedBans = Model.where(Infraction::class.java, "guild", guild.id).where(
                        "active", true).where("type", InfractionType.BAN.internalName).whereIn(
                        "user_id", bannedIds.toTypedArray()).get().map { it.userId }

                val missingInfractions = bannedIds.filter { it !in recordedBans }
                Bot.LOG.debug("Found ${missingInfractions.size} missing infractions")
                missingInfractions.forEach { userId ->
                    val inf = Infraction()
                    inf.type = InfractionType.BAN
                    inf.issuerId = null
                    inf.reason = bans.firstOrNull { it.user.id == userId }?.reason ?: "Unknown"
                    inf.userId = userId
                    inf.guild = guild.id
                    inf.createdAt = Timestamp(System.currentTimeMillis())
                    inf.save()
                }
            }
        }
    }

    fun addMutedRole(user: User, guild: Guild, reason: String = ""): CompletableFuture<Boolean> {
        Bot.LOG.debug("Adding muted role to $user in $guild")
        val role = getMutedRole(guild)
        val future = CompletableFuture<Boolean>()
        if (role == null) {
            guild.kirbotGuild.logManager.genericLog(LogEvent.USER_MUTE, ":warning:",
                    "Cannot assign the muted role to ${user.logName} because the muted role is not configured")
            return CompletableFuture.completedFuture(false)
        }
        val ra = guild.addRoleToMember(user.getMember(guild)!!,
                role)
        if (reason.isNotBlank())
            ra.reason(reason)
        try {
            ra.queue {
                future.complete(true)
            }
        } catch (e: Exception) {
            future.completeExceptionally(e)
        }
        return future
    }

    fun removeMutedRole(user: User, guild: Guild, reason: String = ""): CompletableFuture<Boolean> {
        val member = user.getMember(guild) ?: return CompletableFuture.completedFuture(false)
        val r = member?.roles?.map { it.id }
                ?: return CompletableFuture.completedFuture(false)
        val mutedRole = getMutedRole(guild) ?: return CompletableFuture.completedFuture(false)
        val future = CompletableFuture<Boolean>()
        if (mutedRole.id in r) {
            guild.removeRoleFromMember(member, mutedRole).apply {
                if (reason.isNotBlank())
                    reason(reason)
            }.queue {
                future.complete(true)
            }
        }
        return future
    }

    fun lookupUser(id: String, withId: Boolean = false): String {
        if (id == "1") {
            return "Automatic"
        } else {
            val user = shardManager.getUserById(id)?.nameAndDiscrim
            val model = Model.where(DiscordUser::class.java, "id", id).first()

            if (user != null) {
                return buildString {
                    append(user)
                    if (withId)
                        append(" (`$id`)")
                }
            }
            if (model != null) {
                return buildString {
                    append(model.nameAndDiscrim)
                    if (withId)
                        append(" (`$id`)")
                }
            }
            return id
        }
    }

    fun getMutedRole(guild: Guild): Role? {
        val roleId = GuildSettings.mutedRole.nullableGet(guild) ?: return null
        return guild.getRoleById(roleId)
    }

    fun dmUser(user: String, guild: Guild, infraction: Infraction): CompletableFuture<DmResult> {
        val u = shardManager.getUserById(user)
        if (u != null)
            return dmUser(u, guild, infraction)
        val reason = infraction.reason ?: return CompletableFuture.completedFuture(
                DmResult.NOT_SENT)
        if (!reason.startsWith("[DM]") && !reason.startsWith("[ADM]"))
            return CompletableFuture.completedFuture(DmResult.NOT_SENT)
        else
            return CompletableFuture.completedFuture(DmResult.SEND_ERROR)
    }

    fun dmUser(user: User, guild: Guild, infraction: Infraction): CompletableFuture<DmResult> {
        var reason = infraction.reason ?: return CompletableFuture.completedFuture(
                DmResult.NOT_SENT)
        if (!reason.startsWith("[DM]") && !reason.startsWith("[ADM]"))
            return CompletableFuture.completedFuture(DmResult.NOT_SENT)

        val future = CompletableFuture<DmResult>()
        val anonymous = reason.startsWith("[ADM]")
        reason = reason.replace(Regex("\\[A?DM]"), "")
        try {
            user.openPrivateChannel().queue { channel ->
                val action = when (infraction.type) {
                    InfractionType.WARNING -> "warned"
                    InfractionType.KICK -> "kicked"
                    InfractionType.BAN -> "banned"
                    InfractionType.UNBAN -> "unbanned"
                    InfractionType.MUTE -> "muted"
                    InfractionType.TEMPMUTE -> "temporarily muted"
                    InfractionType.UNKNOWN -> ""
                    InfractionType.WARN -> "warned"
                    InfractionType.TEMPBAN -> "temporarily banned"
                    InfractionType.TEMPROLE -> ""
                }
                channel.sendMessage(buildString {
                    append("You have been ")
                    append(action)
                    when (infraction.type) {
                        InfractionType.WARNING, InfractionType.MUTE, InfractionType.TEMPMUTE -> append(
                                " in **")
                        else -> append(" from **")
                    }
                    append(guild.name)
                    append("**")
                    if (!anonymous) {
                        append(" by ${lookupUser(infraction.issuerId!!, true)}")
                    }
                    if (reason.isNotBlank())
                        append(": `$reason`")
                }).queue {
                    future.complete(DmResult.SENT)
                }
            }
        } catch (e: ErrorResponseException) {
            if (e.errorResponse == ErrorResponse.CANNOT_SEND_TO_USER) {
                return CompletableFuture.completedFuture(DmResult.NOT_SENT)
            }
        }
        return future
    }

    enum class DmResult {
        SENT,
        SEND_ERROR,
        NOT_SENT,
        UNKNOWN
    }
}

data class InfractionResult(val successful: Boolean, val dmResult: Infractions.DmResult,
                            val errorMessage: String? = null)