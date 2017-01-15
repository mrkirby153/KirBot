package me.mrkirby153.KirBot.command;

public enum CommandPermission {

    MRKIRBY153,
    SERVER_ADMIN,
    SERVER_MANAGE_SERVER,
    BOT_ADMIN,
    BOT_MODERATOR,
    DEFAULT;
    public static final String MRKIRBY153_ID = "117791909786812423";

    public boolean has(CommandPermission permission){
        return compareTo(permission) <= 0;
    }
}
