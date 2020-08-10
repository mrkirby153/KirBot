package com.mrkirby153.kirbot.entity.guild.repo

import com.mrkirby153.kirbot.entity.guild.Role
import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<Role, String> {

    fun getAllByServerId(id: String): List<Role>

    fun deleteAllByIdIn(ids: List<String>): Int
}