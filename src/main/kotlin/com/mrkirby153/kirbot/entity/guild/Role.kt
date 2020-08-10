package com.mrkirby153.kirbot.entity.guild

import com.mrkirby153.kirbot.entity.AbstractJpaEntity
import net.dv8tion.jda.api.entities.Role
import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Table

@Entity
@Table(name = "roles")
@EntityListeners(AuditingEntityListener::class)
class Role(id: String,
           @Column(name = "server_id") val serverId: String,
           @Column(name = "name") var name: String,
           @Column(name = "permissions") var permissions: Long,
           // Manually escape order here because it's a reserved keyword
           @Column(name = "\"order\"") var position: Long,
           @Column(name = "created_at") @CreationTimestamp var createdAt: Timestamp? = null,
           @Column(name = "updated_at") @LastModifiedDate var updatedAt: Timestamp? = null) :
        AbstractJpaEntity<String>(id) {


    constructor(role: Role) : this(role.id, role.guild.id, role.name, role.permissionsRaw, role.position.toLong())


    fun update(guildRole: Role): Boolean {
        val changed = guildRole.name != name || guildRole.permissionsRaw != permissions || guildRole.position.toLong() != position
        name = guildRole.name
        permissions = guildRole.permissionsRaw
        position = guildRole.position.toLong()
        return changed
    }
}