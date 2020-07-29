package com.mrkirby153.kirbot.entity.repo

import com.mrkirby153.kirbot.entity.Infraction
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

interface InfractionRepository : CrudRepository<Infraction, Long> {

    fun getAllByUserIdAndGuild(id: String, guild: String): List<Infraction>

    @Query("SELECT inf FROM Infraction inf WHERE inf.userId = (:userId) AND inf.guild = (:guild) AND inf.type = (:type)")
    fun getAllActiveInfractionsByType(userId: String, guild: String, type: Infraction.InfractionType): List<Infraction>
}