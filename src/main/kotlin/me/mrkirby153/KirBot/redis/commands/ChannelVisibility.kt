package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.utils.hide
import me.mrkirby153.KirBot.utils.unhide
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.json.JSONObject

class ChannelVisibility : RedisCommandHandler {
    override fun handle(guild: Guild?, user: User?, json: JSONObject) {
        val chan = json.getString("channel")
        val visible = json.getBoolean("visible")

        if (!visible)
            guild?.getTextChannelById(chan)?.hide()
        else
            guild?.getTextChannelById(chan)?.unhide()
    }

}