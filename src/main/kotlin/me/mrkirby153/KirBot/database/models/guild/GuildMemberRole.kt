package me.mrkirby153.KirBot.database.models.guild

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.PrimaryKey
import me.mrkirby153.KirBot.database.models.Table
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User

@Table("guild_member_roles")
class GuildMemberRole : Model() {

    @PrimaryKey
    var id = ""

    @Column("server_id")
    private var serverId = ""

    @Column("user_id")
    private var userId = ""

    @Column("role_id")
    private var roleId = ""


    val server: Guild?
        get() = Bot.shardManager.getGuild(this.serverId)

    val user: User?
        get() = Bot.shardManager.getUser(this.userId)

    val role: Role?
        get() = server?.getRoleById(this.roleId)
}