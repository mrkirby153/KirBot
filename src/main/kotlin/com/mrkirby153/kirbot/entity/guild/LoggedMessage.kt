package com.mrkirby153.kirbot.entity.guild

import com.mrkirby153.kirbot.entity.AbstractJpaEntity
import net.dv8tion.jda.api.entities.Message
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.sql.Timestamp
import javax.persistence.CascadeType
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.OneToOne
import javax.persistence.Table

@Entity
@Table(name = "server_messages")
@EntityListeners(AuditingEntityListener::class)
class LoggedMessage(id: String,
                    @Column(name = "server_id") val serverId: String,
                    @Column(name = "author") val author: String,
                    @Column(name = "channel") val channel: String,
                    @Column(name = "message") var message: String,
                    @Column(name = "deleted") var deleted: Boolean = false,
                    @Column(name = "edit_count") var editCount: Long = 0L,
                    @Column(name = "created_at") @CreatedDate var createdAt: Timestamp? = null,
                    @Column(name = "updated_at") @LastModifiedDate var updatedAt: Timestamp? = null) :
        AbstractJpaEntity<String>(id) {

    @OneToOne(targetEntity = MessageAttachments::class, cascade = [CascadeType.ALL],
            fetch = FetchType.LAZY)
    @JoinColumn(name = "id", referencedColumnName = "id")
    var attachments: MessageAttachments? = null

    constructor(message: Message) : this(message.id, message.guild.id, message.author.id,
            message.channel.id, message.contentRaw)

    fun update(message: Message) {
        if (message.contentRaw != this.message) {
            this.editCount += 1
            this.message = message.contentRaw
        }
    }

    @Entity
    @Table(name = "attachments")
    @EntityListeners(AuditingEntityListener::class)
    class MessageAttachments(id: String,
                             @Column(name = "attachments") private var attachmentRaw: String?,
                             @Column(name = "created_at") @CreatedDate var createdAt: Timestamp? = null,
                             @Column(name = "updated_at") @LastModifiedDate var updatedAt: Timestamp? = null) :
            AbstractJpaEntity<String>(id) {

        @OneToOne(mappedBy = "attachments")
        lateinit var message: LoggedMessage

        constructor(message: Message) : this(message.id, message.attachments.joinToString(",") { it.url })

        var attachments: List<String>
            get() {
                return attachmentRaw?.split(',') ?: emptyList()
            }
            set(value) {
                attachmentRaw = if (value.isEmpty())
                    null
                else
                    value.joinToString(",")
            }

    }
}