package com.mrkirby153.kirbot.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Table

/**
 * Entity representing guild settings in the database. Guild settings are key/value pairs that
 * can represent arbitrary guild settings.
 */
@Entity
@Table(name = "guild_settings")
@EntityListeners(AuditingEntityListener::class)
class GuildSetting(id: String,
                   @Column(name = "guild") var guild: String,
                   @Column(name = "key") var key: String,
                   @Column(name = "value") var value: String,
                   @CreatedDate @Column(name = "created_at") var createdAt: Timestamp? = null,
                   @LastModifiedDate @Column(name = "updated_at") var updatedAt: Timestamp? = null) :
        AbstractJpaEntity<String>(id)