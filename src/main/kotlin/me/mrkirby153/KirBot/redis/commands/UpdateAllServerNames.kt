package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.realname.RealnameUpdater
import org.json.JSONObject

class UpdateAllServerNames : RedisCommandHandler {

    override fun handle(json: JSONObject) {
        RealnameUpdater().run()
    }
}