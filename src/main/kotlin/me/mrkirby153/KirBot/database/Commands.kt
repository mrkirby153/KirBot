package me.mrkirby153.KirBot.database

import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance

enum class CommandType {
    TEXT,
    JAVASCRIPT
}

data class DBCommand(val id: String, val name: String, val server: Server, val data: String, val clearance: Clearance, val type: CommandType)