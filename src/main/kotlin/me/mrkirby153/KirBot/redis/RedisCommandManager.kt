package me.mrkirby153.KirBot.redis

import me.mrkirby153.KirBot.redis.commands.BotChat
import me.mrkirby153.KirBot.redis.commands.ChannelVisibility
import me.mrkirby153.KirBot.redis.commands.RedisCommandHandler
import me.mrkirby153.KirBot.redis.commands.RolePermissions
import me.mrkirby153.KirBot.redis.commands.SetNickname
import me.mrkirby153.KirBot.redis.commands.SettingChange
import me.mrkirby153.KirBot.redis.commands.SettingDelete
import me.mrkirby153.KirBot.redis.commands.SyncCommand
import me.mrkirby153.KirBot.redis.commands.UpdateLogs

object RedisCommandManager {

    private val commands = mutableMapOf<String, RedisCommandHandler>()


    init {
        register("channel-visibility", ChannelVisibility())
        register("botchat", BotChat())
        register("nickname", SetNickname())
        register("sync", SyncCommand())
        register("setting-update", SettingChange())
        register("settings-delete", SettingDelete())
        register("log-settings", UpdateLogs())
        register("role-clearance", RolePermissions())
    }

    fun getCommand(name: String): RedisCommandHandler? {
        return commands[name.toLowerCase()]
    }

    fun register(name: String, handler: RedisCommandHandler) {
        commands[name.toLowerCase()] = handler
    }
}