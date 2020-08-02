package com.mrkirby153.kirbot.entity

import net.dv8tion.jda.api.entities.User
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.sql.Timestamp
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EntityListeners
import javax.persistence.Table

@Entity
@Table(name = "seen_users")
@EntityListeners(AuditingEntityListener::class)
class DiscordUser(id: String,
                  @Column(name = "username") var username: String,
                  @Column(name = "discriminator") var discriminator: Int,
                  @Column(name = "bot") var bot: Boolean,
                  @Column(name = "created_at") @CreatedDate var createdAt: Timestamp? = null,
                  @Column(name = "updated_at") @LastModifiedDate var updatedAt: Timestamp? = null) :
        AbstractJpaEntity<String>(id) {

    constructor(user: User) : this(user.id, user.name, user.discriminator.toInt(), user.isBot,
            Timestamp.from(user.timeCreated.toInstant()))
}