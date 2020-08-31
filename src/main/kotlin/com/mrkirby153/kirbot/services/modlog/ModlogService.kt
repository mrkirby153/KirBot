package com.mrkirby153.kirbot.services.modlog

import net.dv8tion.jda.api.entities.Guild

/**
 * Service for handling moderation logs
 */
interface ModlogService {

    /**
     * Cache a [guild]'s modlog channels
     */
    fun cache(guild: Guild)

    /**
     * Sets a [guild]'s [hushed] state
     */
    fun hush(guild: Guild, hushed: Boolean = false)

    /**
     * Logs an [event] with the given [message] in a [guild]'s modlogs
     */
    fun log(event: LogEvent, guild: Guild, message: String)
}