package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.infraction.Infraction
import me.mrkirby153.KirBot.infraction.InfractionType
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.audit.ActionType
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.guild.GuildBanEvent
import java.sql.Timestamp

class InfractionModule : Module("infractions") {

    val ignoreBans = mutableListOf<String>()

    override fun onLoad() {

    }

    override fun onGuildBan(event: GuildBanEvent) {
        if (event.user.id in ignoreBans) {
            Bot.LOG.debug("Ignoring ban: ${event.user.id} already created")
            ignoreBans.remove(event.user.id)
            return
        }
        // Create an infraction from the audit logs
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

    private fun findBannedUser(guild: Guild, user: String): User? {
        Bot.LOG.debug("Looking up ban for $user in $guild audit logs")
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
}