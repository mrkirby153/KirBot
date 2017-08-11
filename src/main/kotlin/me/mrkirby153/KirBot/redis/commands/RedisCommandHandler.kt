package me.mrkirby153.KirBot.redis.commands

import org.json.JSONObject

interface RedisCommandHandler {

    fun handle(json: JSONObject)
}