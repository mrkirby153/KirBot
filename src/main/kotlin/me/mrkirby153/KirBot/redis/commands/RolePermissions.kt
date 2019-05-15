package me.mrkirby153.KirBot.redis.commands

import me.mrkirby153.KirBot.utils.kirbotGuild
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import org.json.JSONObject

class RolePermissions : RedisCommandHandler {
    override fun handle(guild: Guild?, user: User?, json: JSONObject) {
        val role = json.optString("role")
        val permission = json.optInt("clearance", -1)
        if (permission != -1) {
            guild?.kirbotGuild?.updateRoleClearance(role, permission)
        } else {
            guild?.kirbotGuild?.deleteRoleClearance(role)
        }
    }
}