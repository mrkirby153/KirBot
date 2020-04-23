package me.mrkirby153.KirBot.database.models.guild


import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import me.mrkirby153.KirBot.logger.LogManager
import java.sql.Timestamp

@Table("attachments")
@Timestamps
class MessageAttachments : Model() {

    var id = ""

    var attachments: String = ""
        get() = LogManager.decrypt(field)

    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @TimestampEnhancer.UpdatedAt
    @Column("updated_at")
    var updatedAt: Timestamp? = null
}