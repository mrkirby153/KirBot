package me.mrkirby153.KirBot.database.models.group

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role

@Table("groups")
class Group : Model() {

    init {
        this.incrementing = false
    }

    @PrimaryKey
    var id = ""

    @Column("server_id")
    private var serverId = ""

    @Column("group_name")
    var name = ""

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
    var role: Role? = null
        get() = server?.getRoleById(this.roleId)
        set(role) {
            this.roleId = role!!.id
            field = role
        }

    val members: List<GroupMember>
        get() = Model.where(GroupMember::class.java, "group_id", this.id).get()
}