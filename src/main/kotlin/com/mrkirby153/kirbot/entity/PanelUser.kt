package com.mrkirby153.kirbot.entity

import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "users")
class PanelUser(id: String, val username: String,
                val admin: Boolean) : AbstractJpaEntity<String>(id)