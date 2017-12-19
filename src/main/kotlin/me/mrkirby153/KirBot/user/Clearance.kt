package me.mrkirby153.KirBot.user

enum class Clearance(val value: Int, val friendlyName: String) {

    /**
     * Users listed in the admins file
     */
    BOT_OWNER(4, "Bot Owner"),

    /**
     * The server owner
     */
    SERVER_OWNER(3, "Server Owner"),

    /**
     * Users with the "Server Administrator" role
     */
    SERVER_ADMINISTRATOR(2, "Server Administrator"),

    /**
     * Users with a "Bot Manager" role
     */
    BOT_MANAGER(1, "Bot Manager"),

    /**
     * Default
     */
    USER(0, "Users"),

    /**
     * Other bots
     */
    BOT(-1, "Bots");
}