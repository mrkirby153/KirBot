package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User

@Table("guild_member_roles")
class GuildMemberRole : Model() {

    init {
        this.incrementing = false
    }

    @PrimaryKey
    var id = ""

    @Column("server_id")
    private var serverId = ""

    @Column("user_id")
    private var userId = ""

    @Column("role_id")
    private var roleId = ""


    @Transient
    var server: Guild? = null
        get() = Bot.shardManager.getGuild(this.serverId)
        set(guild) {
            this.serverId = guild!!.id
            field = guild
        }

    @Transient
    var user: User? = null
        get() = Bot.shardManager.getUser(this.userId)
        set(user) {
            this.userId = user!!.id
            field = user
        }

    @Transient
    var role: Role? = null
        get() = server?.getRoleById(this.roleId)
        set(role) {
            this.roleId = role!!.id
            this.serverId = role.guild.id
            field = role
        }
}