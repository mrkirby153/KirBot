package com.mrkirby153.kirbot.entity.repo

import com.mrkirby153.kirbot.entity.LogChannel
import org.springframework.data.repository.CrudRepository

interface LogChannelRepository : CrudRepository<LogChannel, String> {

    fun getAllByServerId(id: String): List<LogChannel>
}