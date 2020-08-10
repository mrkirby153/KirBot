package com.mrkirby153.kirbot.entity.guild.repo

import com.mrkirby153.kirbot.entity.guild.GuildEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.Optional

interface GuildRepository : JpaRepository<GuildEntity, String> {

    @Modifying
    @Query("UPDATE GuildEntity e SET e.deletedAt = CURRENT_TIMESTAMP WHERE e = (:entity)")
    override fun delete(entity: GuildEntity)

    @Modifying
    @Query("UPDATE GuildEntity e set e.deletedAt = CURRENT_TIMESTAMP")
    override fun deleteAll()

    /**
     * Gets the guild by its id. This only queries guilds that have not been soft-deleted
     */
    @Query("SELECT e FROM GuildEntity e WHERE e.id = :id AND e.deletedAt IS NULL")
    override fun findById(id: String): Optional<GuildEntity>

    @Query("SELECT e from GuildEntity e WHERE e.id = :id")
    fun findByIdWithTrashed(id: String): Optional<GuildEntity>
}