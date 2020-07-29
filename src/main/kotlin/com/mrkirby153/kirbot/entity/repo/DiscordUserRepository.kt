package com.mrkirby153.kirbot.entity.repo

import com.mrkirby153.kirbot.entity.DiscordUser
import org.springframework.data.repository.CrudRepository

interface DiscordUserRepository : CrudRepository<DiscordUser, Long>