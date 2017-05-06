package me.mrkirby153.KirBot.database

import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.entities.Guild

enum class CommandType {
    TEXT,
    JAVASCRIPT
}

data class DBCommand(val id: String, val name: String, val server: Guild, val data: String, val clearance: Clearance, val type: CommandType)