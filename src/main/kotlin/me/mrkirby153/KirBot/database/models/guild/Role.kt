package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.PrimaryKey
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.sharding.ShardManager
import java.sql.Timestamp

@Table("roles")
@Timestamps
class Role(role: net.dv8tion.jda.api.entities.Role? = null) : Model() {

    @PrimaryKey
    var id = ""

    @Column("server_id")
    private var serverId = ""

    var name = ""

    var permissions = 0L

    var order = 0

    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @TimestampEnhancer.UpdatedAt
    @Column("updated_at")
    var updatedAt: Timestamp? = null

    var guild: Guild?
        get() = Bot.applicationContext.get(ShardManager::class.java).getGuildById(serverId)
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
        if (role != null) {
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