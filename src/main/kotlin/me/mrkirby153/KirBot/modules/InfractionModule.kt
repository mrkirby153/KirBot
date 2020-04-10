package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.infraction.Infraction
import me.mrkirby153.KirBot.infraction.InfractionType
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.utils.AuditLogs
import me.mrkirby153.KirBot.utils.Debouncer
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.KirBot.utils.settings.GuildSettings
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.GuildBanEvent
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent
import java.sql.Timestamp

class InfractionModule : Module("infractions") {

    val ignoreBans = mutableListOf<String>()
    val ignoreUnbans = mutableListOf<String>()

    val debouncer = Debouncer()

    override fun onLoad() {

    }

    @Periodic(120)
    fun clearDebouncer() {
        debouncer.removeExpired()
    }

    private fun manualActionsEnabled(guild: Guild): Boolean {
        return GuildSettings.logManualInfractions.get(guild)
    }

    @Subscribe
    fun onGuildBan(event: GuildBanEvent) {
        if (debouncer.find(GuildBanEvent::class.java, Pair("user", event.user.id),
                        Pair("guild", event.guild.id)))
            return
        var actor: User? = null
        var reason = "No reason specified"
        if (manualActionsEnabled(event.guild)) {
            actor = findBannedUser(event.guild, event.user)
            if (actor != event.jda.selfUser) {
                val inf = Infraction()
                inf.issuerId = actor?.id
                inf.userId = event.user.id
                inf.guild = event.guild.id
                inf.createdAt = Timestamp(System.currentTimeMillis())
                inf.type = InfractionType.BAN
                val auditReason = AuditLogs.getReason(event.guild, ActionType.BAN, event.user)
                if (auditReason != null) {
                    reason = auditReason
                }
                inf.reason = auditReason ?: "No reason specified"
                inf.create()
            }
        }
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_BAN, ":rotating_light:",
                buildString {
                    append(event.user.logName).append(" was banned")
                    if (actor != null) {
                        append(" by **${actor.nameAndDiscrim}**")
                    }
                    append(" (`${reason}`)")
                })
    }

    @Subscribe
    fun onGuildUnban(event: GuildUnbanEvent) {
        if (debouncer.find(GuildUnbanEvent::class.java, Pair("user", event.user.id),
                        Pair("guild", event.guild.id)))
            return
        // Revoke any active bans/tempbans on the user
        Infractions.getActiveInfractions(event.user.id,
                event.guild).filter { it.type == InfractionType.BAN || it.type == InfractionType.TEMPBAN }.forEach { ban ->
            ban.revoke()
        }
        var responsibleMember: User? = null
        if (manualActionsEnabled(event.guild)) {
            responsibleMember = findUnbannedUser(event.guild, event.user)
            if (responsibleMember != event.jda.selfUser) {
                Infractions.createInfraction(event.user.id, event.guild,
                        responsibleMember?.id ?: "", "Unbanned", InfractionType.UNBAN)
            }
        }
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_UNBAN, ":hammer:", buildString {
            append(event.user.logName).append(" was unbanned")
            if (responsibleMember != null) {
                append(" by **${responsibleMember.nameAndDiscrim}**")
            }
        })
    }

    @Subscribe
    fun onKick(event: GuildMemberLeaveEvent) {
        if (debouncer.find(GuildMemberLeaveEvent::class.java, Pair("user", event.user.id),
                        Pair("guild", event.guild.id))) {
            return
        }
        if (manualActionsEnabled(event.guild)) {
            // Find the first action of the user being kicked. If it doesn't exist, they did not get kicked but left themselves
            val action = AuditLogs.getFirstAction(event.guild, ActionType.KICK, event.user)
                    ?: return
            if (action.user == event.jda.selfUser)
                return // Ignore ourselves
            Infractions.createInfraction(event.user.id, event.guild, action.user?.id ?: "",
                    action.reason ?: "No reason specified", InfractionType.KICK)
            event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_KICK, ":boot:",
                    buildString {
                        append(event.user.logName)
                        append(" was kicked")
                        if (action.user != null) {
                            append(" by **${action.user!!.nameAndDiscrim}**")
                        }
                        append(": `${action.reason}`")
                    })
        }
    }

    private fun findBannedUser(guild: Guild, user: User): User? {
        Bot.LOG.debug("Looking up ban for $user in $guild audit logs")
        val foundUser: User? = AuditLogs.getActor(guild, ActionType.BAN, user)
        Bot.LOG.debug("Found responsible person: $foundUser")
        return foundUser
    }

    private fun findUnbannedUser(guild: Guild, user: User): User? {
        Bot.LOG.debug("Looking up unban for $user in $guild audit logs")
        return AuditLogs.getActor(guild, ActionType.UNBAN, user)
    }
}