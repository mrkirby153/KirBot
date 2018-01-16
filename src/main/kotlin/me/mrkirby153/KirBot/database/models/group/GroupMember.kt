package me.mrkirby153.KirBot.database.models.group

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.PrimaryKey
import me.mrkirby153.KirBot.database.models.Table
import net.dv8tion.jda.core.entities.User
import java.sql.Timestamp

@Table("user_groups")
class GroupMember : Model() {

    @PrimaryKey
    var id = ""

    @Column("user_id")
    var userId = ""

    @Column("group_id")
    var groupId = ""

    @Column("deleted_at")
    var deletedAt = Timestamp(0)

    val user: User?
        get() = Bot.shardManager.getUser(this.userId)
}