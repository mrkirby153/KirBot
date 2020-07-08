package com.mrkirby153.kirbot.entity

import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Table

/**
 * JPA entity representing a role clearance entity
 */
@Table(name = "role_permissions")
@Entity
@EntityListeners(AuditingEntityListener::class)
class RoleClearance(id: String? = null,
                    @Column(name = "server_id") var serverId: String,
                    @Column(name = "role_id") var roleId: String,
                    @Column(name = "permission_level") var clearanceLevel: Long,
                    @CreatedDate @Column(name = "created_at") var createdAt: Timestamp? = null,
                    @LastModifiedDate @Column(name = "updated_at") var updatedAt: Timestamp? = null) :
        AbstractJpaEntity<String>(id)