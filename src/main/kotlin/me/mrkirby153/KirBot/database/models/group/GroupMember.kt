package me.mrkirby153.KirBot.database.models.group

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.AutoIncrementing
import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.PrimaryKey
import me.mrkirby153.KirBot.database.models.Table
import net.dv8tion.jda.core.entities.User
import java.sql.Timestamp

@Table("user_groups")
@AutoIncrementing(false)
class GroupMember : Model() {

    @PrimaryKey
    var id = ""

    @Column("user_id")
    private var userId = ""

    @Column("group_id")
    var groupId = ""

    @Column("deleted_at")
    var deletedAt = Timestamp(0)

    @Transient
    var user: User? = null
        get() = Bot.shardManager.getUser(this.userId)
        set(user){
            this.userId = user!!.id
            field = user
        }
}