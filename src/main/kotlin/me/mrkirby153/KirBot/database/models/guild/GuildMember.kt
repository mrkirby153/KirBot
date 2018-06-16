package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.Tuple
import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.User

@Table("guild_members")
class GuildMember : Model() {

    init {
        this.incrementing = false
    }

    @PrimaryKey
    var id = ""

    @Column("server_id")
    var serverId = ""

    @Column("user_id")
    var userId = ""

    @Column("user_name")
    var name = ""

    @Column("user_discrim")
    var discrim = ""

    @Column("user_nick")
    var nick: String? = null

    @Transient
    var user: User? = null
        get() = Bot.shardManager.getUser(this.userId)
        set(user) {
            if (user != null) {
                this.userId = user.id
                this.name = user.name
                this.discrim = user.discriminator
            }
            field = user
        }

    val roles: List<GuildMemberRole>
        get() = Model.get(GuildMemberRole::class.java, Tuple("server_id", serverId),
                Tuple("user_id", userId))
}