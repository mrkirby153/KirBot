package me.mrkirby153.KirBot.database.models.guild

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.PrimaryKey
import me.mrkirby153.KirBot.database.models.Table
import net.dv8tion.jda.core.entities.User

@Table("guild_members")
class GuildMember : Model() {

    @PrimaryKey
    var id = ""

    @Column("server_id")
    private var serverId = ""

    @Column("user_id")
    private var userId = ""

    @Column("user_name")
    var name = ""

    @Column("user_discrim")
    var discrim = 0

    @Column("user_nick")
    var nick: String? = null

    @Transient
    var user: User? = null
        get() = Bot.shardManager.getUser(this.userId)
        set(user) {
            this.userId = user!!.id
            field = user
        }

    val roles: List<GuildMemberRole>
        get() = Model.get(GuildMemberRole::class.java, this.userId, "user_id")
}