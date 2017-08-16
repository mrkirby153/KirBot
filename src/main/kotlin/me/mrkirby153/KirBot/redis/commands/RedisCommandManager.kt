package me.mrkirby153.KirBot.redis.commands

object RedisCommandManager {

    private val commands = mutableMapOf<String, RedisCommandHandler>()


    init {
        register("update-name", UpdateNames())
        register("channel-visibility", ChannelVisibility())
    }

    fun getCommand(name: String): RedisCommandHandler? {
        return commands[name.toLowerCase()]
    }

    fun register(name: String, handler: RedisCommandHandler){
        commands[name.toLowerCase()] = handler
    }
}