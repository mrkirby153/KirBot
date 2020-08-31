package com.mrkirby153.kirbot.services.modlog

/**
 * Enum of all log events supported
 */
enum class LogEvent(val friendlyMessage: String, val hushable: Boolean = false) {

    MESSAGE_EDIT("Message edits"),
    MESSAGE_DELETE("Deleted messages", true),
    MESSAGE_BULKDELETE("Bulk deleted messages", true),

    ROLE_ADD("Role added to member"),
    ROLE_REMOVE("Role removed from members"),
    ROLE_CREATE("Role creation"),
    ROLE_DELETE("Role deletion"),
    ROLE_UPDATE("Role updates"),

    USER_JOIN("User joins"),
    USER_LEAVE("User leave"),
    USER_NAME_CHANGE("Username changes"),
    USER_NICKNAME_CHANGE("Nickname changes"),

    ADMIN_COMMAND("Command invoked"),

    SPAM_VIOLATE("Spam filter violations"),
    USER_MUTE("User mutes"),
    USER_UNMUTE("User unmutes"),
    USER_BAN("User banned"),
    USER_UNBAN("User unbanned"),
    USER_KICK("User kick"),
    USER_WARN("User warns"),

    MESSAGE_CENSOR("Censored messages"),
    VOICE_ACTION("Voice channel actions"),
    MEMBER_RESTORE("Member restorations"),

    CHANNEL_CREATE("Channel creation"),
    CHANNEL_MODIFY("Channel modifications"),
    CHANNEL_DELETE("Channel deletions"),

    NAME_CENSOR("Name censors");

    /**
     * The bitflag of a user in the
     */
    val bitflag = 1L.shl(this.ordinal)

    companion object {

        fun calculateBitmask(vararg event: LogEvent): Long {
            var num = 0L
            event.forEach {
                num = num or it.bitflag
            }
            return num
        }

        fun has(raw: Long, event: LogEvent) = (raw and event.bitflag) != 0L

        /**
         * Decodes a [bitmask] into an array of [LogEvent]
         */
        fun decode(bitmask: Long) = values().filter { has(bitmask, it) }
    }
}