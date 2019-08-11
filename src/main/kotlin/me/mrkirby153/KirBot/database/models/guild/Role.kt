package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.api.entities.Guild

@Table("roles")
class Role(role: net.dv8tion.jda.api.entities.Role? = null) : Model() {

    @PrimaryKey
    var id = ""

    @Column("server_id")
    private var serverId = ""

    var name = ""

    var permissions = 0L

    var order = 0

    var guild: Guild?
        get() = Bot.shardManager.getGuildById(serverId)
        set(guild) {
            if (guild == null) {
                return
            }
            this.serverId = guild.id
        }

    var role: net.dv8tion.jda.api.entities.Role?
        get() = guild?.getRoleById(this.id)
        set(role) {
            if (role == null) {
                return
            }
            this.id = role.id
            this.name = role.name
            this.permissions = role.permissionsRaw
            this.order = role.position
        }


    init {
        this.incrementing = false
        if(role != null){
            this.id = role.id
            this.serverId = role.guild.id
            this.name = role.name
            this.permissions = role.permissionsRaw
            this.order = role.position
        }
    }

    fun updateRole() {
        val role = this.role ?: return

        this.name = role.name
        this.permissions = role.permissionsRaw
        this.order = role.position

        save()
    }
}