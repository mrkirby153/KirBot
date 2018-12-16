package me.mrkirby153.KirBot.utils

import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.audit.ActionType
import net.dv8tion.jda.core.audit.AuditLogEntry
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.ISnowflake
import net.dv8tion.jda.core.entities.User

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

    fun getFirstAction(guild: Guild, type: ActionType, id: ISnowflake): AuditLogEntry? {
        val e = guild.auditLogs.type(type).stream().filter { it.targetId == id.id }.findFirst()
        if (e.isPresent)
            return e.get()
        return null
    }
}