package me.mrkirby153.KirBot.redis.commands

object RedisCommandManager {

    private val commands = mutableMapOf<String, RedisCommandHandler>()


    init {
        register("update-name", UpdateNames())
        register("channel-visibility", ChannelVisibility())
        register("update-names", UpdateAllServerNames())
        register("botchat", BotChat())
    }

    fun getCommand(name: String): RedisCommandHandler? {
        return commands[name.toLowerCase()]
    }

    fun register(name: String, handler: RedisCommandHandler){
        commands[name.toLowerCase()] = handler
    }
}