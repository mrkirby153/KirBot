package me.mrkirby153.KirBot.infraction

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.DiscordUser
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
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.guild.GuildBanEvent
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import java.sql.Timestamp
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

object Infractions {

    private var nextTask: ScheduledFuture<*>? = null
    private var nextInfractionId: Long? = null
    private val scheduler = Executors.newSingleThreadScheduledExecutor(
            ThreadFactoryBuilder().setNameFormat("Infraction-Scheduler").setDaemon(true).build())


    fun waitForInfraction() {
        Bot.LOG.debug("Queueing Infractions")
        val nextInfraction = Model.query(Infraction::class.java).whereNotNull("expires_at").where(
                "active", true).orderBy("expires_at", "ASC").limit(1).first()
        if (nextInfraction == null) {
            Bot.LOG.info("No infractions left to wait for")
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
        val guild = Bot.shardManager.getGuild(infraction.guild) ?: return
        val user = Bot.shardManager.getUser(infraction.userId) ?: return
        when (infraction.type) {
            InfractionType.TEMPMUTE -> {
                if (guild.getMember(user) == null) {
                    infraction.revoke()
                    Bot.LOG.debug("User $user is no longer a member of $guild. Skipping infraction")
                    return
                }
                unmute(user.id, guild, guild.jda.selfUser.id, "Timed mute expired")
            }
            InfractionType.TEMPBAN -> {
                ModuleManager[InfractionModule::class.java].ignoreUnbans.add(infraction.userId)
                guild.controller.unban(infraction.userId).queue()
                guild.kirbotGuild.logManager.genericLog(LogEvent.USER_UNBAN, ":rotating_light:",
                        buildString {
                            append(lookupUser(infraction.userId, true))
                            append(" Unbanned by **${lookupUser(guild.selfMember.user.id)}**")
                            append(" (`Timed ban expired`)")
                        })
            }
            InfractionType.TEMPROLE -> {
                // Remove the role from the user
                val roleId = infraction.metadata
                if (roleId != null) {
                    val role = guild.getRoleById(roleId)
                    val member = guild.getMember(user)
                    if (role != null && member != null && role in member.roles) {
                        if (guild.checkPermission(
                                        Permission.MANAGE_ROLES) && guild.selfMember.canAssign(
                                        role)) {
                            ModuleManager[Logger::class.java].debouncer.create(
                                    GuildMemberRoleRemoveEvent::class.java,
                                    Pair("user", infraction.userId), Pair("role", role.id))
                            guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_REMOVE, ":key:",
                                    "Removed **${role.name}** from ${user.logName}: `Temprole expired`")
                            guild.controller.removeSingleRoleFromMember(member, role).reason(
                                    "#${infraction.id} - Temprole expired").queue()
                        }
                    }
                }
            }
            else -> {
                // Not a temporary infraction, don't do anything
            }
        }
        infraction.revoke()
    }

    fun kick(user: String, guild: Guild, issuer: String, reason: String? = null) {
        if (!guild.selfMember.hasPermission(Permission.KICK_MEMBERS)) {
            return
        }
        ModuleManager[Logger::class.java].debouncer.create(GuildMemberLeaveEvent::class.java,
                Pair("user", user))
        guild.controller.kick(guild.getMemberById(user), reason ?: "").queue()

        createInfraction(user, guild, if (issuer == "1") user else issuer, reason,
                InfractionType.KICK)

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_KICK, ":boot:", buildString {
            append(lookupUser(user, true))
            append(" Kicked by **${lookupUser(issuer)}**")
            if (reason != null) {
                append(": `$reason`")
            }
        })
    }

    fun warn(user: String, guild: Guild, issuer: String, reason: String? = null) {
        createInfraction(user, guild, issuer, reason, InfractionType.WARN)
        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_WARN, ":warning:", buildString {
            append(lookupUser(user, true))
            append(" Was warned by **${lookupUser(issuer)}**")
            if (reason != null) {
                append(": `$reason`")
            }
        })
    }

    fun ban(user: String, guild: Guild, issuer: String, reason: String? = null,
            purgeDays: Int = 0) {
        if (!guild.selfMember.hasPermission(Permission.BAN_MEMBERS))
            return
        ModuleManager[InfractionModule::class.java].debouncer.create(GuildBanEvent::class.java,
                Pair("user", user), Pair("guild", guild.id))
        ModuleManager[Logger::class.java].debouncer.create(GuildMemberLeaveEvent::class.java,
                Pair("user", user))
        guild.controller.ban(user, purgeDays, reason).queue()

        createInfraction(user, guild, if (issuer == "1") user else issuer, reason,
                InfractionType.BAN)

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_BAN, ":rotating_light:", buildString {
            append(lookupUser(user, true))
            append(" Banned by **${lookupUser(issuer)}**")
            if (reason != null)
                append(": `$reason`")
        })
    }

    fun softban(user: String, guild: Guild, issuer: String, reason: String? = null) {
        if (!guild.selfMember.hasPermission(Permission.BAN_MEMBERS))
            return
        ModuleManager[InfractionModule::class.java].debouncer.create(GuildBanEvent::class.java,
                Pair("user", user), Pair("guild", guild.id))
        ModuleManager[InfractionModule::class.java].debouncer.create(GuildUnbanEvent::class.java,
                Pair("user", user), Pair("guild", guild.id))
        ModuleManager[Logger::class.java].debouncer.create(GuildMemberLeaveEvent::class.java,
                Pair("user", user))
        guild.controller.ban(user, 7, reason).queue {
            guild.controller.unban(user).queue()
        }

        createInfraction(user, guild, if (issuer == "1") user else issuer, reason,
                InfractionType.KICK)

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_KICK, ":boot:", buildString {
            append(lookupUser(user, true))
            append(" Soft-Banned by **${lookupUser(issuer)}**")
            if (reason != null)
                append(": `$reason`")
        })
    }

    fun unban(user: String, guild: Guild, issuer: String, reason: String = "") {
        if (!guild.selfMember.hasPermission(Permission.BAN_MEMBERS))
            return
        ModuleManager[InfractionModule::class.java].debouncer.create(GuildUnbanEvent::class.java,
                Pair("user", user), Pair("guild", guild.id))
        guild.banList.queue { banList ->
            if (user in banList.map { it.user.id })
                guild.controller.unban(user).queue()
            else
                Bot.LOG.debug("$user is not banned so not unbanning")
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
                    append(" Unbanned by **${lookupUser(issuer)}**")
                    if (reason.isNotBlank())
                        append("(`$reason`)")
                })
    }

    fun mute(user: String, guild: Guild, issuer: String, reason: String? = null,
             createInfraction: Boolean = true) {
        if (!guild.selfMember.hasPermission(Permission.MANAGE_ROLES))
            return
        addMutedRole(guild.getMemberById(user).user, guild)

        if (createInfraction)
            createInfraction(user, guild, if (issuer == "1") user else issuer, reason,
                    InfractionType.MUTE)

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_MUTE, ":zipper_mouth:", buildString {
            append(lookupUser(user, true))
            append(" Muted by **${lookupUser(issuer)}**")
            if (reason != null) {
                append(": `$reason`")
            }
        })
    }

    fun unmute(user: String, guild: Guild, issuer: String, reason: String? = null) {
        if (!guild.selfMember.hasPermission(Permission.MANAGE_ROLES))
            return
        removeMutedRole(guild.getMemberById(user).user, guild)

        getActiveInfractions(user,
                guild).filter { it.type == InfractionType.MUTE || it.type == InfractionType.TEMPMUTE }.forEach { ban ->
            ban.revoke()
        }

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_UNMUTE, ":open_mouth:", buildString {
            append(lookupUser(user, true))
            append(" Unmuted by **${lookupUser(issuer)}**")
            if (reason != null) {
                append(": `$reason`")
            }
        })
    }

    fun tempMute(user: String, guild: Guild, issuer: String, duration: Long, units: TimeUnit,
                 reason: String? = null) {
        if (!guild.selfMember.hasPermission(Permission.MANAGE_ROLES))
            return
        addMutedRole(guild.getMemberById(user).user, guild)
        val inf = createInfraction(user, guild, if (issuer == "1") user else issuer, reason,
                InfractionType.TEMPMUTE, Timestamp.from(
                Instant.now().plusMillis(TimeUnit.MILLISECONDS.convert(duration, units))))

        waitForInfraction()

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_MUTE, ":zipper_mouth:", buildString {
            append(lookupUser(user, true))
            append(" Temp muted by **${lookupUser(issuer)}** for ${Time.formatLong(
                    TimeUnit.MILLISECONDS.convert(duration, units), Time.TimeUnit.SECONDS)}")
            if (reason != null) {
                append(": `$reason`")
            }
        })
    }

    fun tempban(user: String, guild: Guild, issuer: String, duration: Long, units: TimeUnit,
                reason: String? = null, purgeDays: Int = 0) {
        if (!guild.selfMember.hasPermission(Permission.BAN_MEMBERS))
            return
        ModuleManager[InfractionModule::class.java].ignoreBans.add(user)
        guild.banList.queue { banList ->
            if (user !in banList.map { it.user.id })
                guild.controller.ban(user, purgeDays, reason).queue()
        }
        val inf = createInfraction(user, guild, issuer, reason, InfractionType.TEMPBAN,
                Timestamp.from(
                        Instant.now().plusMillis(TimeUnit.MILLISECONDS.convert(duration, units))))
        waitForInfraction()
        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_BAN, ":rotating_light:", buildString {
            append(lookupUser(user, true))
            append(" Temp-banned for ${Time.formatLong(
                    TimeUnit.MILLISECONDS.convert(duration, units))} by **${lookupUser(issuer)}**")
            if (reason != null)
                append(": `$reason`")
        })
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
            guild.banList.queue { bans ->
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

    fun addMutedRole(user: User, guild: Guild) {
        Bot.LOG.debug("Adding muted role to $user in $guild")
        val role = getMutedRole(guild)
        if (role == null) {
            guild.kirbotGuild.logManager.genericLog(LogEvent.USER_MUTE, ":warning:",
                    "Cannot assign the muted role to ${user.logName} because the muted role is not configured")
            return
        }
        guild.controller.addSingleRoleToMember(user.getMember(guild),
                role).queue()
    }

    fun removeMutedRole(user: User, guild: Guild) {
        val r = user.getMember(guild)?.roles?.map { it.id } ?: return
        val mutedRole = getMutedRole(guild) ?: return
        if (mutedRole.id in r) {
            guild.controller.removeSingleRoleFromMember(user.getMember(guild), mutedRole).queue()
        }
    }

    fun lookupUser(id: String, withId: Boolean = false): String {
        if (id == "1") {
            return "Automatic"
        } else {
            val user = Bot.shardManager.getUser(id)?.nameAndDiscrim
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
        return guild.kirbotGuild.settings.mutedRole
    }
}