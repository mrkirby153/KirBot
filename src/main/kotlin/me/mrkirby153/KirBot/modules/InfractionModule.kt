package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.infraction.Infraction
import me.mrkirby153.KirBot.infraction.InfractionType
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.utils.Debouncer
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.audit.ActionType
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.guild.GuildBanEvent
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent
import java.sql.Timestamp

class InfractionModule : Module("infractions") {

    val ignoreBans = mutableListOf<String>()
    val ignoreUnbans = mutableListOf<String>()

    val debouncer = Debouncer()

    override fun onLoad() {

    }

    @Periodic(120)
    fun clearDebouncer(){
        debouncer.removeExpired()
    }

    override fun onGuildBan(event: GuildBanEvent) {
        if (debouncer.find(GuildBanEvent::class.java, Pair("user", event.user.id),
                        Pair("guild", event.guild.id)))
            return
        // Create an infraction from the audit logs if we can view the banlist
        if (event.guild.selfMember.hasPermission(Permission.BAN_MEMBERS))
            event.guild.banList.queue { banList ->
                val entry = banList.firstOrNull { it.user.id == event.user.id } ?: return@queue
                val infraction = Infraction()
                val actor = findBannedUser(event.guild, event.user.id)
                infraction.issuerId = actor?.id
                infraction.userId = event.user.id
                infraction.guild = event.guild.id
                infraction.createdAt = Timestamp(System.currentTimeMillis())
                infraction.type = InfractionType.BAN
                infraction.reason = entry.reason ?: "No reason specified"
                infraction.create()
                Bot.LOG.debug("Created infraction ${infraction.id}")
                event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_BAN, ":rotating_light:",
                        "${event.user.nameAndDiscrim} (`${event.user.id}`) was banned by **${actor?.nameAndDiscrim
                                ?: "Unknown"}** (`${entry.reason}`)")
            }
    }

    override fun onGuildUnban(event: GuildUnbanEvent) {
        if (debouncer.find(GuildUnbanEvent::class.java, Pair("user", event.user.id),
                        Pair("guild", event.guild.id)))
            return
        Infractions.getActiveInfractions(event.user.id,
                event.guild).filter { it.type == InfractionType.BAN }.forEach { ban ->
            ban.revoke()
        }
        val responsibleMember = findUnbannedUser(event.guild, event.user.id)
        Infractions.createInfraction(event.user.id, event.guild, responsibleMember?.id ?: "1",
                "Manually Revoked", InfractionType.UNBAN)
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_UNBAN, ":hammer:",
                "${event.user.nameAndDiscrim} (`${event.user.id}`) Was unbanned by **${responsibleMember?.nameAndDiscrim
                        ?: "Unknown"}**")
    }

    private fun findBannedUser(guild: Guild, user: String): User? {
        Bot.LOG.debug("Looking up ban for $user in $guild audit logs")
        if (!guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            Bot.LOG.debug("Cannot view audit logs, not looking up user")
            return null
        }
        var foundUser: User? = null
        val entries = guild.auditLogs.type(ActionType.BAN).complete()
        entries.forEach { e ->
            Bot.LOG.debug("Found ban for ${e.targetId}")
            if (e.targetId == user && foundUser == null) {
                foundUser = e.user
            }
        }
        Bot.LOG.debug("Found responsible person: $foundUser")
        return foundUser
    }

    private fun findUnbannedUser(guild: Guild, user: String): User? {
        Bot.LOG.debug("Looking up unban for $user in $guild audit logs")
        if (!guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS)) {
            Bot.LOG.debug("Cannot view audit logs, not looking up user")
            return null
        }
        var found: User? = null
        val entries = guild.auditLogs.type(ActionType.UNBAN).complete()
        entries.forEach {
            if (it.targetId == user && found == null)
                found = it.user
        }
        return found
    }
}