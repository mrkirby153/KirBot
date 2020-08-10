package com.mrkirby153.kirbot.entity.guild.repo

import com.mrkirby153.kirbot.entity.guild.Channel
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.TextChannel
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface ChannelRepository : CrudRepository<Channel, String> {

    fun getAllByServerId(serverId: String): List<Channel>

    fun deleteAllByIdIn(id: Collection<String>): Int
}