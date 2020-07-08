package com.mrkirby153.kirbot.entity.repo

import com.mrkirby153.kirbot.entity.RoleClearance
import org.springframework.data.repository.CrudRepository

interface RoleClearanceRepository : CrudRepository<RoleClearance, String> {

    fun getAllByRoleIdIn(roles: List<String>): List<RoleClearance>
}