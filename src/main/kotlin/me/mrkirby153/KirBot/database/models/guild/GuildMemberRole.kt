package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.kcutils.utils.IdGenerator
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.User

@Table("guild_member_roles")
class GuildMemberRole(member: Member? = null, role: Role? = null) : Model() {

    @PrimaryKey
    var id = ""

    @Column("server_id")
    private var serverId = ""

    @Column("user_id")
    var userId = ""

    @Column("role_id")
    var roleId = ""


    var server: Guild?
        get() = Bot.shardManager.getGuildById(this.serverId)
        set(guild) {
            this.serverId = guild!!.id
        }

    var user: User?
        get() = Bot.shardManager.getUserById(this.userId)
        set(user) {
            this.userId = user!!.id
        }

    var role: Role?
        get() = server?.getRoleById(this.roleId)
        set(role) {
            this.roleId = role!!.id
            this.serverId = role.guild.id
        }


    init {
        this.incrementing = false
        if(member != null && role != null){
            this.id = idGenerator.generate(10)
            this.serverId = member.guild.id
            this.userId = member.user.id
            this.roleId = role.id
        }
    }

    companion object {
        private val idGenerator = IdGenerator(IdGenerator.ALPHA + IdGenerator.NUMBERS)
    }
}