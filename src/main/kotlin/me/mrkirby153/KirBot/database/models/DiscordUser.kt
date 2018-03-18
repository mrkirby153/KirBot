package me.mrkirby153.KirBot.database.models

import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.User

@Table("seen_users")
class DiscordUser : Model() {

    var id: String = ""

    var username: String = ""

    var discriminator: Int = 0

    @Transient
    var user: User? = null
        get() = Bot.shardManager.getUser(this.id)
}