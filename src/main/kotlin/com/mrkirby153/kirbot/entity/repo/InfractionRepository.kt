package com.mrkirby153.kirbot.entity.repo

import com.mrkirby153.kirbot.entity.Infraction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.sql.Timestamp
import java.util.Optional

interface InfractionRepository : JpaRepository<Infraction, Long> {

    fun getAllByUserIdAndGuild(id: String, guild: String): List<Infraction>

    @Query("SELECT inf FROM Infraction inf WHERE inf.userId = (:userId) AND inf.guild = (:guild) AND inf.type = (:type)")
    fun getAllActiveInfractionsByType(userId: String, guild: String, type: Infraction.InfractionType): List<Infraction>

    @Query("SELECT inf from Infraction inf WHERE inf.expiresAt IS NOT NULL AND inf.active = true ORDER BY inf.expiresAt DESC")
    fun getNextInfractionToExpire(): Optional<Infraction>

    @Query("SELECT inf FROM Infraction inf WHERE inf.expiresAt < (:timestamp) AND inf.active = true")
    fun getAllInfractionsExpiringBefore(timestamp: Timestamp): List<Infraction>
}