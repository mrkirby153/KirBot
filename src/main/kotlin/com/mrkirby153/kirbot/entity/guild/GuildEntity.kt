package com.mrkirby153.kirbot.entity.guild

import com.mrkirby153.kirbot.entity.AbstractJpaEntity
import net.dv8tion.jda.api.entities.Guild
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Table

@Entity
@Table(name = "guild")
@EntityListeners(AuditingEntityListener::class)
class GuildEntity(id: String,
                  @Column(name = "name") var name: String,
                  @Column(name = "icon_id") var iconId: String?,
                  @Column(name = "owner") var owner: String?,
                  @Column(name = "created_at") @CreationTimestamp var createdAt: Timestamp? = null,
                  @Column(name = "updated_at") @UpdateTimestamp var updatedAt: Timestamp? = null,
                  @Column(name = "deleted_at") var deletedAt: Timestamp? = null) :
        AbstractJpaEntity<String>(id) {

    constructor(guild: Guild): this(guild.id, guild.name, guild.iconId, guild.ownerId)

    /**
     * Updates this guild entity with the properties from the provided [guild]. Returns true
     * if the entity has changed and needs to be persisted
     */
    fun update(guild: Guild): Boolean {
        val changed = guild.name != name || guild.iconId != iconId
        name = guild.name
        iconId = guild.iconId
        return changed
    }

    /**
     * Restores the given entity. The entity **MUST** be saved to persist this change to the db
     */
    fun restore() {
        deletedAt = null
    }
}