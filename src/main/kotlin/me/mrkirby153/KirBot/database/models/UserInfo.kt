package me.mrkirby153.KirBot.database.models

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.User

@Table("user_info")
class UserInfo : Model() {

    var id = ""

    @Column("first_name")
    var firstName = ""

    @Column("last_name")
    var lastName = ""

    val user: User?
        get() = Bot.shardManager.getUser(this.id)
}