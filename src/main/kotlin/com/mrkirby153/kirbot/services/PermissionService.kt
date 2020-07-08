package com.mrkirby153.kirbot.services

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import java.util.concurrent.TimeUnit

/**
 * Service responsible for determining permissions for a user in a guild
 */
interface PermissionService {

    /**
     * Gets the clearance of a user
     *
     * @param user The user to get the clearance of
     * @param guild The guild to get the clearance on
     * @return The user's clearance on the provided guild. If the user is not in the guild, 0 is
     * returned.
     */
    fun getClearance(user: User, guild: Guild): Long

    /**
     * Gets the clearance of a user. If the user does not have any clearance explicitly defined via
     * any of their roles, they will have a clearance level of `0` by default. If a user has multiple
     * roles with clearances, this will return the highest clearance of their roles.
     *
     * @param member The member to get the clearance for
     * @return The member's clearance
     */
    fun getClearance(member: Member): Long

    /**
     * **Global Admin Usage Only**
     *
     * Overrides the clearance of the provided user for the provided time. Calls to [getClearance]
     * will return [Long.MAX_VALUE]
     *
     * @param user The user to override the clearance for
     * @param time The time to override clearance for
     * @param unit The time unit for the provided time
     * @return The unix time (in ms) that the overridden clearance expires in
     */
    fun overrideClearance(user: User, time: Long = 5, unit: TimeUnit = TimeUnit.MINUTES): Long

    /**
     * **Global Admin Usage Only**
     *
     * Clears the overridden clearance of the provided user. Calls to [getClearance] will return
     * their "correct" values
     *
     * @param user The user to clear the clearance for
     */
    fun clearOverriddenClearance(user: User)

    /**
     * Checks if the given user is a global admin
     *
     * @param user The user to check for global admin
     * @return True if the user is a global admin
     */
    fun isGlobalAdmin(user: User): Boolean
}