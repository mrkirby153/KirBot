package com.mrkirby153.kirbot.entity.repo

import com.mrkirby153.kirbot.entity.CommandAlias
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.util.Optional

interface CommandAliasRepository : JpaRepository<CommandAlias, String> {

    /**
     * Gets the default clearance for a guild
     *
     * @param guildId The guild id to get the default clearance to
     * @return The default clearance for the guild
     */
    @Query("SELECT c.clearance FROM CommandAlias c WHERE c.serverId = :guildId AND c.command = '*'")
    fun getDefaultClearance(guildId: String): Optional<Long>

    /**
     * Gets the command alias by its case insensitive name on a guild
     *
     * @param command The command
     * @param guildId The guild id
     * @return The command alias if it exists
     */
    fun getCommandAliasByCommandIgnoringCaseAndServerId(command: String,
                                            guildId: String): Optional<CommandAlias>
}