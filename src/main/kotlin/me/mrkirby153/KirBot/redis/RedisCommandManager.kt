package me.mrkirby153.KirBot.redis

import me.mrkirby153.KirBot.redis.commands.BotChat
import me.mrkirby153.KirBot.redis.commands.ChannelVisibility
import me.mrkirby153.KirBot.redis.commands.RedisCommandHandler
import me.mrkirby153.KirBot.redis.commands.SetNickname
import me.mrkirby153.KirBot.redis.commands.SyncCommand

object RedisCommandManager {

    private val commands = mutableMapOf<String, RedisCommandHandler>()


    init {
        register("channel-visibility", ChannelVisibility())
        register("botchat", BotChat())
        register("nickname", SetNickname())
        register("sync", SyncCommand())
    }

    fun getCommand(name: String): RedisCommandHandler? {
        return commands[name.toLowerCase()]
    }

    fun register(name: String, handler: RedisCommandHandler) {
        commands[name.toLowerCase()] = handler
    }
}