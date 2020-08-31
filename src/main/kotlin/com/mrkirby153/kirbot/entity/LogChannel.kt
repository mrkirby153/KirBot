package com.mrkirby153.kirbot.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Table

@Entity
@Table(name = "log_settings")
@EntityListeners(AuditingEntityListener::class)
class LogChannel(id: String,
                 @Column(name = "server_id") val serverId: String,
                 @Column(name = "channel_id") val channelId: String,
                 @Column(name = "included") var included: Long,
                 @Column(name = "excluded") var excluded: Long,
                 @Column(name = "created_at") @CreatedDate var createdAt: Timestamp? = null,
                 @Column(name = "updated_at") @LastModifiedDate var updatedAt: Timestamp? = null) :
        AbstractJpaEntity<String>(id)