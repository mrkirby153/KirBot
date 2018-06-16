package me.mrkirby153.KirBot.database.models.guild

import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.PrimaryKey
import me.mrkirby153.KirBot.database.models.Table
import me.mrkirby153.KirBot.logger.LogManager

@Table("server_messages")
class GuildMessage : Model() {

    @PrimaryKey
    var id = ""

    @Column("server_id")
    var serverId = ""

    var author = ""

    var channel = ""

    var message = ""
        get() = LogManager.decrypt(field)
        set(value) {
            field = LogManager.encrypt(value)
        }

    var deleted: Boolean = false

    @Column("edit_count")
    var editCount = 0

    var attachments: String? = null
        get() = if (field != null) LogManager.decrypt(field!!) else null
        set(value) {
            field = if (value != null) LogManager.encrypt(value) else null
        }
}