package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.CompletableFuture

@Table("reaction_roles")
class ReactionRole : Model() {

    init {
        incrementing = false
    }

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