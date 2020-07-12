package com.mrkirby153.kirbot.entity.repo

import com.mrkirby153.kirbot.entity.GuildSetting
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface GuildSettingRepository : CrudRepository<GuildSetting, String> {

    fun getByGuildAndKey(guild: String, key: String): Optional<GuildSetting>

    fun deleteByGuildAndKey(guild: String, key: String): Int
}