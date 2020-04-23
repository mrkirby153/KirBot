package me.mrkirby153.KirBot.database.models.guild


import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.PrimaryKey
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.kcutils.utils.IdGenerator
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import java.sql.Timestamp

@Table("guild_members")
@Timestamps
class GuildMember(member: Member? = null) : Model() {

    @PrimaryKey
    var id = ""

    @Column("server_id")
    var serverId = ""

    @Column("user_id")
    var userId = ""

    @Column("user_name")
    var name = ""

    @Column("user_discrim")
    var discrim = ""

    @Column("user_nick")
    var nick: String? = null

    var deafened = false
    var muted = false

    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @TimestampEnhancer.UpdatedAt
    @Column("updated_at")
    var updatedAt: Timestamp? = null

    var user: User?
        get() = Bot.applicationContext.get(ShardManager::class.java).getUserById(this.userId)
        set(user) {
            if (user != null) {
                this.userId = user.id
                this.name = user.name
                this.discrim = user.discriminator
            }
        }

    init {
        if (member != null) {
            this.id = idGenerator.generate(10)
            serverId = member.guild.id
            userId = member.user.id
            nick = member.nickname
            name = member.user.name
            discrim = member.user.discriminator
            muted = member.voiceState?.isGuildMuted ?: false
            deafened = member.voiceState?.isDeafened ?: false
        }
    }

    val roles: List<GuildMemberRole>
        get() = Model.where(GuildMemberRole::class.java, "server_id", serverId).where("user_id",
                userId).get()

    fun updateMember() {
        val guild = Bot.applicationContext.get(ShardManager::class.java).getGuildById(this.serverId) ?: return
        val member = guild.getMemberById(this.userId)
        if (member == null) {
            Bot.LOG.debug("Guild member $this was not found (left the guild?) deleting")
            delete()
            return
        }

        this.name = member.user.name
        this.discrim = member.user.discriminator
        this.nick = member.nickname
        this.save()
    }


    companion object {
        private val idGenerator = IdGenerator(IdGenerator.ALPHA + IdGenerator.NUMBERS)
    }
}