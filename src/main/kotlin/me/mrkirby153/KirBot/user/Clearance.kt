package me.mrkirby153.KirBot.user

enum class Clearance(val value: Int) {

    /**
     * Users listed in the admins file
     */
    BOT_OWNER(4),

    /**
     * The server owner
     */
    SERVER_OWNER(3),

    /**
     * Users with the "Server Administrator" role
     */
    SERVER_ADMINISTRATOR(2),

    /**
     * Users with a "Bot Manager" role
     */
    BOT_MANAGER(1),

    /**
     * Default
     */
    USER(0),

    /**
     * Other bots
     */
    BOT(-1)
}