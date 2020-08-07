package com.mrkirby153.kirbot.entity.guild

import com.mrkirby153.kirbot.entity.AbstractJpaEntity
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.lang.IllegalArgumentException
import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.Table

@Entity
@Table(name = "channels")
@EntityListeners(AuditingEntityListener::class)
class Channel(id: String,
              @Column(name = "server") val serverId: String,
              @Column(name = "channel_name") var name: String,
              @Column(name = "type") @Enumerated(EnumType.STRING) val type: Type,
              @Column(name = "hidden") var hidden: Boolean,
              @Column(name = "created_at") @CreationTimestamp var createdAt: Timestamp? = null,
              @Column(name = "updated_at") @LastModifiedDate var updatedAt: Timestamp? = null) :
        AbstractJpaEntity<String>(id) {

    constructor(channel: GuildChannel) : this(channel.id, channel.guild.id, channel.name, when(channel) {
        is TextChannel -> Type.TEXT
        is VoiceChannel -> Type.VOICE
        else -> throw IllegalArgumentException("Unrecognized channel type")
    }, channel.getPermissionOverride(channel.guild.publicRole)?.denied?.contains(Permission.MESSAGE_READ) ?: false)

    /**
     * Updates this channel with the provided [channel]
     */
    fun update(channel: GuildChannel) : Boolean {
        val isHidden = channel.getPermissionOverride(channel.guild.publicRole)?.denied?.contains(
                Permission.MESSAGE_READ) ?: false
        val changed = channel.name != this.name || this.hidden != isHidden
        this.name = channel.name
        this.hidden = isHidden
        return changed
    }

    enum class Type(val column: String) {
        TEXT("TEXT"),
        VOICE("VOICE")
    }
}