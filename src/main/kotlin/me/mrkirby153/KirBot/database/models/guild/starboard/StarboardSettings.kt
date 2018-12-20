package me.mrkirby153.KirBot.database.models.guild.starboard

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.TextChannel

@Table("starboard_settings")
class StarboardSettings : Model() {

    init {
        incrementing = false
    }

    var id: String = ""

    var enabled: Boolean = false

    @Column("star_count")
    var starCount = 0L

    @Column("gild_count")
    var gildCount = 0L

    @Column("self_star")
    var selfStar: Boolean = false

    @Column("channel_id")
    var channelId: String? = null

    val channel: TextChannel?
        get() {
            val guild = Bot.shardManager.getGuild(this.id) ?: throw IllegalArgumentException(
                    "Could not find guild with id ${this.id}")
            return guild.getTextChannelById(this.channelId)
        }
}