package me.mrkirby153.KirBot.infraction

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.executors.moderation.infraction.TempMute
import me.mrkirby153.KirBot.database.models.DiscordUser
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.QueryBuilder
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.InfractionModule
import me.mrkirby153.KirBot.modules.Scheduler
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User
import java.sql.Timestamp
import java.util.concurrent.TimeUnit

object Infractions {

    fun kick(user: String, guild: Guild, issuer: String, reason: String? = null) {
        guild.controller.kick(guild.getMemberById(user), reason ?: "").queue()

        createInfraction(user, guild, if(issuer == "1") user else issuer, reason, InfractionType.KICK)

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_KICK, ":boot:", buildString {
            append(lookupUser(user, true))
            append(" Kicked by **${lookupUser(issuer)}**")
            if (reason != null) {
                append(": `$reason`")
            }
        })
    }

    fun ban(user: String, guild: Guild, issuer: String, reason: String? = null,
            purgeDays: Int = 0) {
        ModuleManager[InfractionModule::class.java].ignoreBans.add(user)
        guild.controller.ban(user, purgeDays, reason).queue()

        createInfraction(user, guild, if(issuer == "1") user else issuer, reason, InfractionType.BAN)

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_BAN, ":rotating_light:", buildString {
            append(lookupUser(user, true))
            append(" Banned by **${lookupUser(issuer)}**")
            if (reason != null)
                append(": `$reason`")
        })
    }

    fun softban(user: String, guild: Guild, issuer: String, reason: String? = null) {
        guild.controller.ban(user, 7, reason).queue {
            guild.controller.unban(user).queue()
        }

        createInfraction(user, guild, if(issuer == "1") user else issuer, reason, InfractionType.KICK)

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_KICK, ":boot:", buildString {
            append(lookupUser(user, true))
            append(" Soft-Banned by **${lookupUser(issuer)}**")
            if (reason != null)
                append(": `$reason`")
        })
    }

    fun unban(user: String, guild: Guild, issuer: String, reason: String = "") {
        ModuleManager[InfractionModule::class.java].ignoreUnbans.add(user)
        guild.controller.unban(user).queue()

        // Deactivate all the users active bans (should only be one)
        getActiveInfractions(user, guild).filter { it.type == InfractionType.BAN }.forEach { ban ->
            ban.active = false
            ban.revokedAt = Timestamp(System.currentTimeMillis())
            ban.save()
        }
        createInfraction(user, guild, if(issuer == "1") user else issuer, reason, InfractionType.UNBAN)
        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_UNBAN, ":hammer:", buildString {
            append(lookupUser(user, true))
            append(" Unbanned by **${lookupUser(issuer)}**")
            if (reason.isNotBlank())
                append("(`$reason`)")
        })
    }

    fun mute(user: String, guild: Guild, issuer: String, reason: String? = null) {
        addMutedRole(guild.getMemberById(user).user, guild)
        createInfraction(user, guild, if(issuer == "1") user else issuer, reason, InfractionType.MUTE)

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_MUTE, ":zipper_mouth:", buildString {
            append(lookupUser(user, true))
            append(" Muted by **${lookupUser(issuer)}**")
            if (reason != null) {
                append(": `$reason`")
            }
        })
    }

    fun unmute(user: String, guild: Guild, issuer: String, reason: String? = null) {
        removeMutedRole(guild.getMemberById(user).user, guild)

        Model.get(Infraction::class.java, Pair("user_id", user), Pair("guild", guild.id),
                Pair("type", "mute"), Pair("active", true)).forEach {
            it.active = false
            it.revokedAt = Timestamp(System.currentTimeMillis())
            it.save()
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
        addMutedRole(guild.getMemberById(user).user, guild)
        val inf = createInfraction(user, guild, if(issuer == "1") user else issuer, reason, InfractionType.TEMPMUTE)

        ModuleManager[Scheduler::class.java].submit(
                TempMute.UnmuteScheduler(inf.id.toString(), user, guild.id),
                duration, units)
        val m = guild.getMemberById(user)

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_MUTE, ":zipper_mouth:", buildString {
            append(lookupUser(user, true))
            append(" Temp muted by **${lookupUser(issuer)}** for ${Time.formatLong(
                    TimeUnit.MILLISECONDS.convert(duration, units), Time.TimeUnit.SECONDS)}")
            if (reason != null) {
                append(": `$reason`")
            }
        })
    }

    fun createInfraction(user: String, guild: Guild, issuer: String, reason: String?,
                         type: InfractionType): Infraction {
        val infraction = Infraction()
        infraction.userId = user
        infraction.guild = guild.id
        infraction.issuerId = issuer
        infraction.reason = reason
        infraction.type = type
        infraction.createdAt = Timestamp(System.currentTimeMillis())
        infraction.save()
        return infraction
    }

    fun createInfraction(user: String, guild: Guild, issuer: User, reason: String?,
                         type: InfractionType): Infraction {
        return createInfraction(user, guild, issuer.id, reason, type)
    }

    fun getActiveInfractions(user: String, guild: Guild? = null): List<Infraction> {
        return Model.get(Infraction::class.java,
                Pair("user_id", user)).filter { it.active }.filter {
            if (guild != null)
                guild.id == it.guild
            else
                true
        }
    }

    fun getAllInfractions(user: User, guild: Guild? = null): List<Infraction> {
        return Model.get(Infraction::class.java, Pair("user_id", user.id)).filter {
            if (guild != null)
                guild.id == it.guild
            else
                true
        }
    }


    fun importFromBanlist(guild: KirBotGuild) {
        guild.banList.queue { bans ->
            bans.filter {
                !QueryBuilder(Infraction::class).where("user_id", it.user.id).where(
                        "type", "ban").exists()
            }.forEach {
                val infraction = Infraction()
                infraction.type = InfractionType.BAN
                infraction.issuerId = null
                infraction.reason = it.reason
                infraction.userId = it.user.id
                infraction.guild = guild.id
                infraction.createdAt = Timestamp(System.currentTimeMillis())
                infraction.save()
            }
        }
    }

    fun addMutedRole(user: User, guild: Guild) {
        guild.controller.addSingleRoleToMember(user.getMember(guild),
                getOrCreateMutedRole(guild)).queue()
    }

    fun removeMutedRole(user: User, guild: Guild) {
        val r = user.getMember(guild).roles.map { it.id }
        val mutedRole = getOrCreateMutedRole(guild)
        if (mutedRole.id in r) {
            guild.controller.removeSingleRoleFromMember(user.getMember(guild), mutedRole).queue()
        }
    }

    fun lookupUser(id: String, withId: Boolean = false): String {
        if (id == "1") {
            return "Automatic"
        } else {
            val user = Bot.shardManager.getUser(id)?.nameAndDiscrim
            val model = Model.first(DiscordUser::class.java, id)

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

    private fun getOrCreateMutedRole(guild: Guild): Role {
        val role = guild.roles.firstOrNull { it.name.equals("muted", true) }
        if (role == null) {
            Bot.LOG.debug("Muted role does not exist on $guild")
            val r = guild.controller.createRole().complete()
            val kbRoles = guild.selfMember.roles
            var highest = kbRoles.first().position
            kbRoles.forEach {
                if (it.position > highest)
                    highest = it.position
            }
            r.manager.setName("Muted").queue {
                guild.controller.modifyRolePositions().selectPosition(r).moveTo(highest - 1).queue()
                guild.textChannels.forEach { chan ->
                    val o = chan.getPermissionOverride(r)
                    if (o == null) {
                        chan.createPermissionOverride(r).setDeny(Permission.MESSAGE_WRITE).queue()
                    }
                }
            }
            return r
        } else {
            // Check the permissions
            guild.textChannels.forEach { chan ->
                val o = chan.getPermissionOverride(role)
                if (o == null && chan.checkPermissions(Permission.MANAGE_PERMISSIONS)) {
                    chan.createPermissionOverride(role).setDeny(Permission.MESSAGE_WRITE).queue()
                }
            }
            return role
        }
    }
}