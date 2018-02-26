package me.mrkirby153.KirBot.infraction

import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.QueryBuilder
import me.mrkirby153.KirBot.server.KirBotGuild
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import java.sql.Timestamp

object Infractions {

    fun kick(user: User, guild: Guild, issuer: User, reason: String? = null) {
        guild.controller.kick(guild.getMember(user), reason ?: "").queue()

        createInfraction(user.id, guild, issuer, reason, InfractionType.KICK)
    }

    fun ban(user: String, guild: Guild, issuer: User, reason: String? = null, purgeDays: Int = 7) {
        guild.controller.ban(user, purgeDays, reason).queue()

        createInfraction(user, guild, issuer, reason, InfractionType.BAN)
    }

    fun softban(user: String, guild: Guild, issuer: User, reason: String? = null) {
        guild.controller.ban(user, 7, reason).queue {
            guild.controller.unban(user).queue()
        }

        createInfraction(user, guild, issuer, reason, InfractionType.KICK)
    }

    fun unban(user: String, guild: Guild, issuer: User) {
        guild.controller.unban(user).queue()

        // Deactivate all the users active bans (should only be one)
        getActiveInfractions(user, guild).filter { it.type == InfractionType.BAN }.forEach { ban ->
            ban.active = false
            ban.revokedAt = Timestamp(System.currentTimeMillis())
            ban.save()
        }
    }

    fun createInfraction(user: String, guild: Guild, issuer: User, reason: String?,
                                 type: InfractionType): Infraction {
        val infraction = Infraction()
        infraction.userId = user
        infraction.guild = guild.id
        infraction.issuerId = issuer.id
        infraction.reason = reason
        infraction.type = type
        infraction.createdAt = Timestamp(System.currentTimeMillis())
        infraction.save()
        return infraction
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
}