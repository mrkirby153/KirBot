package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.kcutils.utils.IdGenerator
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User

@Table("guild_members")
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

    @Transient
    var user: User? = null
        get() = Bot.shardManager.getUser(this.userId)
        set(user) {
            if (user != null) {
                this.userId = user.id
                this.name = user.name
                this.discrim = user.discriminator
            }
            field = user
        }

    init {
        this.incrementing = false
        if (member != null) {
            this.id = idGenerator.generate(10)
            serverId = member.guild.id
            userId = member.user.id
            nick = member.nickname
            name = member.user.name
            discrim = member.user.discriminator
        }
    }

    val roles: List<GuildMemberRole>
        get() = Model.where(GuildMemberRole::class.java, "server_id", serverId).where("user_id",
                userId).get()

    fun updateMember() {
        val guild = Bot.shardManager.getGuild(this.serverId) ?: return
        val member = guild.getMemberById(this.userId) ?: return

        this.name = member.user.name
        this.discrim = member.user.discriminator
        this.nick = member.nickname
        this.save()
    }


    companion object {
        private val idGenerator = IdGenerator(IdGenerator.ALPHA + IdGenerator.NUMBERS)
    }
}