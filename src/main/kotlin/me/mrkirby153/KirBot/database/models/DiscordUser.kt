package me.mrkirby153.KirBot.database.models

import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.User

@Table("seen_users")
class DiscordUser(user: User? = null) : Model() {

    var id: String = ""

    var username: String = ""

    var discriminator: Int = 0

    @Transient
    var user: User? = null
        get() = Bot.shardManager.getUser(this.id)

    val nameAndDiscrim: String
        get() = "$username#$discriminator"


    init {
        incrementing = false
        if (user != null) {
            this.id = user.id
            this.username = user.name
            this.discriminator = user.discriminator.toInt()
        }
    }

    fun updateUser(){
        val u = Bot.shardManager.getUser(this.id) ?: return
        this.username = u.name
        this.discriminator = u.discriminator.toInt()
        this.save()
    }
}