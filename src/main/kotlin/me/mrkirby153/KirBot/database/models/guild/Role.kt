package me.mrkirby153.KirBot.database.models.guild

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.AutoIncrementing
import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.PrimaryKey
import me.mrkirby153.KirBot.database.models.Table
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role

@Table("roles")
@AutoIncrementing(false)
class Role : Model() {

    @PrimaryKey
    var id = ""

    @Column("server_id")
    private var serverId = ""

    var name = ""

    var permissions = 0L

    var order = 0

    @Transient
    var guild: Guild? = null
        get() = Bot.shardManager.getGuild(serverId)
        set(guild) {
            this.serverId = guild!!.id
            field = guild
        }

    @Transient
    var role: Role? = null
        get() = guild?.getRoleById(this.id)
        set(role) {
            this.id = role!!.id
            this.name = role.name
            this.permissions = role.permissionsRaw
            this.order = role.position
            field = role
        }

    fun updateRole() {
        val role = this.role ?: return

        this.name = role.name
        this.permissions = role.permissionsRaw
        this.order = role.position

        save()
    }
}