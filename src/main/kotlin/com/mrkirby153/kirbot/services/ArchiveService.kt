package com.mrkirby153.kirbot.services

import java.net.URL
import java.util.concurrent.CompletableFuture

/**
 * Service for handling archives
 */
interface ArchiveService {

    /**
     * Uploads the given [text] to the archive. Returns a [CompletableFuture] with the [URL] of
     * the archive. Archives persist for 7 days unless [ttl] is specified
     */
    fun uploadToArchive(text: String, ttl: Long = 604800): CompletableFuture<URL>
}