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
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.GuildBanEvent
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent
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

    @Subscribe
    fun onGuildBan(event: GuildBanEvent) {
        if (debouncer.find(GuildBanEvent::class.java, Pair("user", event.user.id),
                        Pair("guild", event.guild.id)))
            return
        // Create an infraction from the audit logs if we can view the banlist
        if (event.guild.selfMember.hasPermission(Permission.BAN_MEMBERS)) {
            event.guild.retrieveBanList().queue { banList ->
                val entry = banList.firstOrNull { it.user.id == event.user.id } ?: return@queue
                val actor = findBannedUser(event.guild, event.user)
                val inf = Infraction()
                inf.issuerId = actor?.id
                inf.userId = event.user.id
                inf.guild = event.guild.id
                inf.createdAt = Timestamp(System.currentTimeMillis())
                inf.type = InfractionType.BAN
                inf.reason = entry.reason ?: "No reason specified"
                inf.create()
                event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_BAN, ":rotating_light:",
                        "${event.user.logName} was banned by **${actor?.nameAndDiscrim
                                ?: "Unknown"}** (`${entry.reason}`)")
            }
        }
    }

    @Subscribe
    fun onGuildUnban(event: GuildUnbanEvent) {
        if (debouncer.find(GuildUnbanEvent::class.java, Pair("user", event.user.id),
                        Pair("guild", event.guild.id)))
            return
        Infractions.getActiveInfractions(event.user.id,
                event.guild).filter { it.type == InfractionType.BAN }.forEach { ban ->
            ban.revoke()
        }
        val responsibleMember = findUnbannedUser(event.guild, event.user)
        Infractions.createInfraction(event.user.id, event.guild, responsibleMember?.id ?: "1",
                "Manually Revoked", InfractionType.UNBAN)
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_UNBAN, ":hammer:",
                "${event.user.nameAndDiscrim} (`${event.user.id}`) Was unbanned by **${responsibleMember?.nameAndDiscrim
                        ?: "Unknown"}**")
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