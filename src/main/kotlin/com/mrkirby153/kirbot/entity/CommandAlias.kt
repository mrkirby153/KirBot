package com.mrkirby153.kirbot.entity

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


/**
 * Entity representing a command alias
 */
@Entity
@Table(name = "command_aliases")
class CommandAlias(id: String,
                   @Column(name = "server_id") var serverId: String,
                   @Column(name = "command") var command: String,
                   @Column(name = "alias") var alias: String?,
                   @Column(name = "clearance") var clearance: Long,
                   @CreatedDate @Column(name = "created_at") var createdAt: Timestamp? = null,
                   @LastModifiedDate @Column(name = "updated_at") var updatedAt: Timestamp? = null) :
        AbstractJpaEntity<String>(id)