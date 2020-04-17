package me.mrkirby153.KirBot.database.models

import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager

@Table("seen_users")
class DiscordUser(user: User? = null) : Model() {

    var id: String = ""

    var username: String = ""

    var discriminator: Int = 0

    var bot: Boolean = false

    var user: User?
        get() = Bot.applicationContext.get(ShardManager::class.java).getUserById(this.id)
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
        val u = Bot.applicationContext.get(ShardManager::class.java).getUserById(this.id) ?: return
        this.username = u.name
        this.discriminator = u.discriminator.toInt()
        this.bot = u.isBot
        this.save()
    }
}