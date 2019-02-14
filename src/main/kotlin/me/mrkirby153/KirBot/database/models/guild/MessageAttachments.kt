package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.logger.LogManager

@Table("attachments")
class MessageAttachments : Model() {

    var id = ""

    var attachments: String = ""
        get() = LogManager.decrypt(field)
        set(value) {
            field = LogManager.encrypt(value)
        }
}