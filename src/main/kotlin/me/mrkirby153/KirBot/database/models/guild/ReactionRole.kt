package me.mrkirby153.KirBot.database.models.guild


import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.PrimaryKey
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.sharding.ShardManager
import java.sql.Timestamp
import java.util.concurrent.CompletableFuture

@Table("reaction_roles")
@Timestamps
class ReactionRole : Model() {

    @PrimaryKey
    var id: String = ""

    @Column("guild_id")
    var guildId: String = ""

    @Column("role_id")
    var roleId: String = ""

    @Column("channel_id")
    var channelId: String = ""

    @Column("message_id")
    var messageId: String = ""

    @Column("custom_emote")
    var custom: Boolean = false

    var emote: String = ""

    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @TimestampEnhancer.UpdatedAt
    @Column("updated_at")
    var updatedAt: Timestamp? = null


    var guild: Guild?
        get() = Bot.applicationContext.get(ShardManager::class.java).getGuildById(this.guildId)
        set(value) {
            if (value != null) {
                guildId = value.id
            }
        }

    var role: Role?
        get() = guild?.getRoleById(this.roleId)
        set(value) {
            if (value != null) {
                guildId = value.guild.id
                roleId = value.id
            }
        }

    var channel: TextChannel?
        get() = guild?.getTextChannelById(this.channelId)
        set(value) {
            if (value != null) {
                guildId = value.guild.id
                channelId = value.id
            }
        }

    val message: CompletableFuture<Message>?
        get() = channel?.retrieveMessageById(messageId)?.submit()

    val displayString: String
        get() {
            if (custom) {
                val emote = guild?.getEmoteById(this.emote) ?: return this.emote
                return emote.asMention
            } else {
                return this.emote
            }
        }
}