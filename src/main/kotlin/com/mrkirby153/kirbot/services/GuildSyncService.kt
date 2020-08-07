package com.mrkirby153.kirbot.services

import net.dv8tion.jda.api.entities.Guild
import java.util.concurrent.CompletableFuture

/**
 * Service responsible for syncing a guilds information in the database
 */
interface GuildSyncService {

    /**
     * Synchronizes the provided [guild]. Returns a [CompletableFuture] completed when the guild
     * has finished synchronizing
     */
    fun sync(guild: Guild): CompletableFuture<Void>
}