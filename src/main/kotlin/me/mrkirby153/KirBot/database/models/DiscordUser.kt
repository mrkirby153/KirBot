package me.mrkirby153.KirBot.database.models

import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.api.entities.User

@Table("seen_users")
class DiscordUser(user: User? = null) : Model() {

    var id: String = ""

    var username: String = ""

    var discriminator: Int = 0

    var bot: Boolean = false

    var user: User?
        get() = Bot.shardManager.getUserById(this.id)
        set(user) {
            this.id = user?.id ?: ""
        }

    val nameAndDiscrim: String
        get() = "$username#$discriminator"


    init {
        incrementing = false
        if (user != null) {
            this.id = user.id
            this.username = user.name
            this.discriminator = user.discriminator.toInt()
            this.bot = user.isBot
        }
    }

    fun updateUser(){
        val u = Bot.shardManager.getUserById(this.id) ?: return
        this.username = u.name
        this.discriminator = u.discriminator.toInt()
        this.bot = u.isBot
        this.save()
    }
}