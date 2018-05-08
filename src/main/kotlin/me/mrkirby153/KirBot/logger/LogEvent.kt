package me.mrkirby153.KirBot.logger

enum class LogEvent(val permission: Long) {

    // A message was edited
    MESSAGE_EDIT(1),
    // A message deleted
    MESSAGE_DELETE(2),
    // Multiple messages were deleted
    MESSAGE_BULKDELETE(4),

    // A role was given to a user
    ROLE_ADD(8),
    // A role was removed from a user
    ROLE_REMOVE(16),
    // A role was created
    ROLE_CREATE(32),
    // A role was deleted
    ROLE_DELETE(64),
    // A role was updated
    ROLE_UPDATE(128),

    // A user joins the guild
    USER_JOIN(256),
    // A user leaves the guild
    USER_LEAVE(512),
    // A user changes their username
    USER_NAME_CHANGE(1024),
    // A user changes their nickname
    USER_NICKNAME_CHANGE(2048),

    // A command was run that should log to the modlogs
    ADMIN_COMMAND(4096),

    // A user has violated the spam filter
    SPAM_VIOLATE(8192),
    // A user was muted
    USER_MUTE(16384),
    // A user was unmuted
    USER_UNMUTE(32768),
    // A user was banned
    USER_BAN(65536),
    // A user was unbanned
    USER_UNBAN(131072),
    USER_KICK(262144),

    // A message by a user was censored
    MESSAGE_CENSOR(524288);

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