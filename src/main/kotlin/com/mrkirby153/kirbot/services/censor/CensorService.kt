package com.mrkirby153.kirbot.services.censor

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User

/**
 * Service for handling censorship of messages
 */
interface CensorService {

    /**
     * Gets all the [CensorRule]s configured on the provided [guild]
     */
    fun getRules(guild: Guild): List<CensorSetting>

    /**
     * Gets a list of [CensorRule]s that apply to the provided [user] on the provided [guild]
     */
    fun getEffectiveRules(guild: Guild, user: User): List<CensorSetting>

    /**
     * Checks if the provided [msg] violates any of the guild's configured Censor Rules.
     * @return A list of violations, or an empty list of no violations occurred
     */
    fun check(msg: Message): List<ViolationException>
}