package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.model.SoftDeletingModel
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.sharding.ShardManager
import java.sql.Timestamp

@Table("guild")
@Timestamps
class DiscordGuild(guild: Guild? = null) : SoftDeletingModel() {

    var id = ""

    var name = ""

    @Column("icon_id")
    var iconId: String? = ""

    var owner = ""

    val ownerUser
        get() = Bot.applicationContext.get(ShardManager::class.java).getUserById(this.owner)


    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @TimestampEnhancer.UpdatedAt
    @Column("updated_at")
    var updatedAt: Timestamp? = null

    @SoftDeleteField
    @Column("deleted_at")
    var deletedAt: Timestamp? = null


    init {
        if (guild != null) {
            this.id = guild.id
            this.name = guild.name
            this.iconId = guild.iconId
            this.owner = guild.ownerId
        }
    }

}