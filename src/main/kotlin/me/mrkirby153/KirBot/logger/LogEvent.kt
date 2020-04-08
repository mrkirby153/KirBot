package me.mrkirby153.KirBot.logger

enum class LogEvent {

    // A message was edited
    MESSAGE_EDIT,
    // A message deleted
    MESSAGE_DELETE,
    // Multiple messages were deleted
    MESSAGE_BULKDELETE,

    // A role was given to a user
    ROLE_ADD,
    // A role was removed from a user
    ROLE_REMOVE,
    // A role was created
    ROLE_CREATE,
    // A role was deleted
    ROLE_DELETE,
    // A role was updated
    ROLE_UPDATE,

    // A user joins the guild
    USER_JOIN,
    // A user leaves the guild
    USER_LEAVE,
    // A user changes their username
    USER_NAME_CHANGE,
    // A user changes their nickname
    USER_NICKNAME_CHANGE,

    // A command was run that should log to the modlogs
    ADMIN_COMMAND,

    // A user has violated the spam filter
    SPAM_VIOLATE,
    // A user was muted
    USER_MUTE,
    // A user was unmuted
    USER_UNMUTE,
    // A user was banned
    USER_BAN,
    // A user was unbanned
    USER_UNBAN,
    USER_KICK,

    USER_WARN,

    // A message by a user was censored
    MESSAGE_CENSOR,

    VOICE_ACTION,

    MEMBER_RESTORE,

    CHANNEL_CREATE,
    CHANNEL_MODIFY,
    CHANNEL_DELETE,

    NAME_CENSOR;

    val permission = 1L.shl(this.ordinal)

    companion object {

        fun calculatePermissions(vararg events: LogEvent): Long {
            var num = 0L

            events.forEach {
                num = num or it.permission
            }

            return num
        }

        fun has(permissionRaw: Long, permission: LogEvent): Boolean {
            return (permissionRaw and permission.permission) != 0L
        }
    }
}