package me.mrkirby153.KirBot.database.models.group

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.AutoIncrementing
import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.PrimaryKey
import me.mrkirby153.KirBot.database.models.Table
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role

@Table("groups")
@AutoIncrementing(false)
class Group : Model(){

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
        get() = Model.get(GroupMember::class.java, this.id, "group_id")
}