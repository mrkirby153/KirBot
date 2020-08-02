package com.mrkirby153.kirbot.events

import com.mrkirby153.kirbot.entity.Infraction
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User

/**
 * The base Infraction event. All other infraction events extend from this class
 */
open class InfractionEvent(
        /**
         * The infraction that was issued
         */
        val infraction: Infraction?,
        /**
         * The user that the infraction was issued against
         */
        val user: User?,
        /**
         * The guild that the infraction was issued in
         */
        val guild: Guild)

/**
 * Base infraction for temporary infractions
 */
open class TempInfractionEvent(infraction: Infraction, user: User?, guild: Guild,
                               /**
                                * When the infraction expires
                                */
                               val expiresOn: Long) :
        InfractionEvent(infraction, user, guild)

/**
 * Event fired when a user is kicked
 */
class UserKickEvent(infraction: Infraction, user: User, guild: Guild) :
        InfractionEvent(infraction, user, guild)

/**
 * Event fired when a user is banned
 */
class UserBanEvent(infraction: Infraction, val userId: String, user: User?, guild: Guild) :
        InfractionEvent(infraction, user, guild)

/**
 * Event fired when a user is temporarily banned
 */
class UserTempBanEvent(infraction: Infraction, val userId: String, user: User?, guild: Guild, expiresOn: Long) :
        TempInfractionEvent(infraction, user, guild, expiresOn)

/**
 * Event fired when a user is muted
 */
class UserMuteEvent(infraction: Infraction?, user: User, guild: Guild) :
        InfractionEvent(infraction, user, guild)

/**
 * Event fired when a user is unmuted
 */
class UserUnmuteEvent(user: User, guild: Guild): InfractionEvent(null, user, guild)

/**
 * Event fired when a user is temporarily muted
 */
class UserTempMuteEvent(infraction: Infraction, user: User, guild: Guild, expiresOn: Long) :
        TempInfractionEvent(infraction, user, guild, expiresOn)

/**
 * Event fired when a user is warned
 */
class UserWarnEvent(infraction: Infraction, val userId: String, user: User?, guild: Guild) :
        InfractionEvent(infraction, user, guild)

/**
 * Event fired when a user is unbanned
 */
class UserUnbanEvent(infraction: Infraction, val userId: String, guild: Guild) : InfractionEvent(infraction, null, guild)