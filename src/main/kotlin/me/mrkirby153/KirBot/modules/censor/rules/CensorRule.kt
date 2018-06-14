package me.mrkirby153.KirBot.modules.censor.rules

import net.dv8tion.jda.core.entities.Message
import org.json.JSONObject

interface CensorRule {
    fun check(message: Message, config: JSONObject)
}