package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.infraction.Infraction
import me.mrkirby153.KirBot.infraction.InfractionType
import me.mrkirby153.KirBot.module.Module
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
            infraction.issuerId = null
            infraction.userId = event.user.id
            infraction.guild = event.guild.id
            infraction.createdAt = Timestamp(System.currentTimeMillis())
            infraction.type = InfractionType.BAN
            infraction.reason = entry.reason ?: "No reason specified"
            infraction.create()
            Bot.LOG.debug("Created infraction ${infraction.id}")
        }
    }
}