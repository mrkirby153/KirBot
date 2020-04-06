package me.mrkirby153.KirBot.utils

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.audit.ActionType
import net.dv8tion.jda.api.audit.AuditLogEntry
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.ISnowflake
import net.dv8tion.jda.api.entities.User
import java.time.Instant


object AuditLogs {

    /**
     * Gets the actor of an action in a guild
     *
     * @param guild The guild
     * @param actionType The action type
     * @param target The target
     * @return The [User] or `null`
     */
    fun getActor(guild: Guild, actionType: ActionType, target: ISnowflake): User? {
        if (!guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS))
            return null
        return getFirstAction(guild, actionType, target)?.user
    }

    fun getReason(guild: Guild, actionType: ActionType, target: ISnowflake): String? {
        if (!guild.selfMember.hasPermission(Permission.VIEW_AUDIT_LOGS))
            return null
        return getFirstAction(guild, actionType, target)?.reason
    }

    fun getFirstAction(guild: Guild, type: ActionType, id: ISnowflake,
                       period: Long = 30): AuditLogEntry? {
        val e = guild.retrieveAuditLogs().type(type).stream().filter {
            it.targetId == id.id && it.timeCreated.toInstant().isAfter(
                    Instant.now().minusSeconds(period))
        }.findFirst()
        if (e.isPresent)
            return e.get()
        return null
    }
}