package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.PrimaryKey
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import me.mrkirby153.KirBot.logger.LogManager
import net.dv8tion.jda.api.entities.Message
import java.sql.Timestamp

@Table("server_messages")
@Timestamps
class GuildMessage(message: Message? = null) : Model() {

    @PrimaryKey
    var id = ""

    @Column("server_id")
    var serverId = ""

    var author = ""

    var channel = ""

    var message = ""
        get() = LogManager.decrypt(field)

    var deleted: Boolean = false

    @Column("edit_count")
    var editCount = 0

    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @Column("updated_at")
    var updatedAt: Timestamp? = null


    @Transient
    var pendingAttachments: String? = null

    var attachments: String?
        get() {
            return Model.where(MessageAttachments::class.java, "id", this.id).first()?.attachments
        }
        set(value) {
            // TODO 2019-03-31 Fix bug where these are inserted before the modal actually exists
            if(!this.exists()) {
                this.pendingAttachments = value
                return;
            }
            val existing = Model.where(MessageAttachments::class.java, "id", this.id).first()
            if (existing != null) {
                if (value == null) {
                    existing.delete()
                } else {
                    existing.attachments = value
                    existing.save()
                }
            } else {
                if (value != null) {
                    val newAttachments = MessageAttachments()
                    newAttachments.id = this.id
                    newAttachments.attachments = value
                    newAttachments.save()
                }
            }
        }


    init {
        if (message != null) {
            this.id = message.id
            this.serverId = message.guild.id
            this.author = message.author.id
            this.channel = message.channel.id
            this.message = message.contentRaw
            this.attachments = if (message.attachments.size > 0) message.attachments.joinToString(
                    ",") { it.url } else null
            this.createdAt = Timestamp.from(message.timeCreated.toInstant())
            this.updatedAt = Timestamp.from(message.timeEdited?.toInstant() ?: message.timeCreated.toInstant())
        }
    }

    override fun save() {
        super.save()
        if(this.pendingAttachments != null) {
            this.attachments = this.pendingAttachments
        }
    }
}