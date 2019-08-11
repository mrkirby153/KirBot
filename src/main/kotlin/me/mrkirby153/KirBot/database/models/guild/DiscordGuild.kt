package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.SoftDeletingModel
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.api.entities.Guild

@Table("guild")
class DiscordGuild(guild: Guild? = null) : SoftDeletingModel() {

    var id = ""

    var name = ""

    @Column("icon_id")
    var iconId: String? = ""

    var owner = ""

    val ownerUser
        get() = Bot.shardManager.getUserById(this.owner)


    init {
        incrementing = false
        if (guild != null) {
            this.id = guild.id
            this.name = guild.name
            this.iconId = guild.iconId
            this.owner = guild.ownerId
        }
    }

}