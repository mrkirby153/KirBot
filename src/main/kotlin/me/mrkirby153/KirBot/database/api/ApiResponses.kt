package me.mrkirby153.KirBot.database.api

import net.dv8tion.jda.core.entities.User

data class Realname(val firstName: String, val lastName: String) : ApiResponse

data class Realnames(val map: MutableMap<User, Realname>): ApiResponse