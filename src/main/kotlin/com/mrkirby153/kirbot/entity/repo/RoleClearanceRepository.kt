package com.mrkirby153.kirbot.entity.repo

import com.mrkirby153.kirbot.entity.RoleClearance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository

interface RoleClearanceRepository : JpaRepository<RoleClearance, String> {

    fun getAllByRoleIdIn(roles: List<String>): List<RoleClearance>
}