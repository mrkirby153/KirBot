package me.mrkirby153.KirBot.database.models.guild


import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.PrimaryKey
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.kcutils.utils.IdGenerator
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import java.sql.Timestamp

@Table("guild_member_roles")
@Timestamps
class GuildMemberRole(member: Member? = null, role: net.dv8tion.jda.api.entities.Role? = null) : Model() {

    @PrimaryKey
    var id = ""

    @Column("server_id")
    private var serverId = ""

    @Column("user_id")
    var userId = ""

    @Column("role_id")
    var roleId = ""

    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @TimestampEnhancer.UpdatedAt
    @Column("updated_at")
    var updatedAt: Timestamp? = null


    var server: Guild?
        get() = Bot.applicationContext.get(ShardManager::class.java).getGuildById(this.serverId)
        set(guild) {
            this.serverId = guild!!.id
        }

    var user: User?
        get() = Bot.applicationContext.get(ShardManager::class.java).getUserById(this.userId)
        set(user) {
            this.userId = user!!.id
        }

    var role: net.dv8tion.jda.api.entities.Role?
        get() = server?.getRoleById(this.roleId)
        set(role) {
            this.roleId = role!!.id
            this.serverId = role.guild.id
        }


    init {
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