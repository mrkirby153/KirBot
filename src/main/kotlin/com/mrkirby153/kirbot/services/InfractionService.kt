package com.mrkirby153.kirbot.services

import com.mrkirby153.kirbot.entity.Infraction
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

/**
 * Service for handling user infractions. This service is responsible for recording infractions
 * in the database, handling expiring infractions, as well as actually performing the requested
 * infraction
 */
interface InfractionService {

    /**
     * Kicks the user from the guild
     *
     * @param infractionContext The infraction context
     * @return A completable future of the dm result
     */
    fun kick(infractionContext: InfractionContext): CompletableFuture<InfractionResult>

    /**
     * Bans the user from the guild
     *
     * @param infractionContext The infraction context
     * @return A completable future of the dm result
     */
    fun ban(infractionContext: InfractionContext): CompletableFuture<InfractionResult>

    /**
     * Temporarily bans the user from the guild
     *
     * @param infractionContext The infraction context
     * @param duration The duration given in [units]
     * @param units The units
     * @return A completable future of the dm result
     */
    fun tempBan(infractionContext: InfractionContext, duration: Long,
                units: TimeUnit): CompletableFuture<InfractionResult>

    /**
     * Mutes the user in the guild
     *
     * @param infractionContext The infraction context
     * @return A completable future of the dm result
     */
    fun mute(infractionContext: InfractionContext): CompletableFuture<InfractionResult>

    /**
     * Umutes the user in a guild
     *
     * @param infractionContext The infraction context
     * @return A completable ftuure of the dm result
     */
    fun unmute(infractionContext: InfractionContext): CompletableFuture<InfractionResult>

    /**
     * Unbans the user in a guild
     *
     * @param guild The guild to unban the user on
     * @param user The user Id being unbanned
     * @param issuer The person unbanning the user
     * @param reason The reason why they're being unbanned
     * @return A completable future of the dm result
     */
    fun unban(guild: Guild, user: String, issuer: User, reason: String): CompletableFuture<InfractionResult>

    /**
     * Temporarily mutes the user in the guild
     *
     * @param infractionContext The infraction context
     * @param duration The duration given in [units]
     * @param units The units
     * @return A completable future of the dm result
     */
    fun tempMute(infractionContext: InfractionContext, duration: Long,
                 units: TimeUnit): CompletableFuture<InfractionResult>

    /**
     * Warns the user in the guild
     *
     * @param infractionContext The infraction context
     * @return A completable future of the dm result
     */
    fun warn(infractionContext: InfractionContext): CompletableFuture<InfractionResult>

    /**
     * Gets all the infractions for the provided user
     *
     * @param user The user to get the infractions for
     * @param guild The guild to retrieve the infractions in
     */
    fun getInfractions(user: User, guild: Guild): List<Infraction>

    /**
     * DTO for providing a unified interface for all infraction related commands
     */
    data class InfractionContext(
            /**
             * The user receiving the infraction
             */
            val user: User,
            /**
             * The guild that the infraction is being issued on
             */
            val guild: Guild,
            /**
             * The user issuing the infraction
             */
            val issuer: User,
            /**
             * The reason for the infraction
             */
            val reason: String?)

    /**
     * The result of the infraction
     */
    data class InfractionResult(
            /**
             * The result of attempting to send a dm to the user. If a DM was not requested,
             * [DmResult.NOT_SENT] is returned.
             */
            val dmResult: DmResult,
            /**
             * The infraction that was created as a result of the action
             */
            val infraction: Infraction?)

    /**
     * The result of sending the DM to a user for an infraction
     */
    enum class DmResult {
        SENT,
        SEND_ERROR,
        NOT_SENT,
        UNKNOWN
    }
}