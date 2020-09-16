package com.mrkirby153.kirbot.entity.repo

import com.mrkirby153.kirbot.entity.LogChannel
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.CrudRepository

interface LogChannelRepository : JpaRepository<LogChannel, String> {

    fun getAllByServerId(id: String): List<LogChannel>
}