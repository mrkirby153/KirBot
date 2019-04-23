package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.logger.LogManager
import net.dv8tion.jda.core.entities.Message
import java.sql.Timestamp

@Table("server_messages")
class GuildMessage(message: Message? = null) : Model() {

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


    @Transient
    var pendingAttachments: String? = null

    var attachments: String?
        get() {
            return Model.where(MessageAttachments::class.java, "id", this.id).first()?.attachments
        }
        set(value) {
            // TODO 2019-03-31 Fix bug where these are inserted before the modal actually exists
            if(!this.exists) {
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
        this.incrementing = false
        if (message != null) {
            this.id = message.id
            this.serverId = message.guild.id
            this.author = message.author.id
            this.channel = message.channel.id
            this.message = message.contentRaw
            this.attachments = if (message.attachments.size > 0) message.attachments.joinToString(
                    ",") { it.url } else null
            this.createdAt = Timestamp.from(message.creationTime.toInstant())
            this.updatedAt = this.createdAt
        }
    }

    override fun save() {
        super.save()
        if(this.pendingAttachments != null) {
            this.attachments = this.pendingAttachments
        }
    }
}