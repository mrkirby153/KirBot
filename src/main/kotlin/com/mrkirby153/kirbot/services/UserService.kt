package com.mrkirby153.kirbot.services

import java.util.concurrent.CompletableFuture

/**
 * Service to keep track of users and find them using various methods
 */
interface UserService {

    /**
     * Finds the given [User] for the provided [id]. The future is completed with the user or
     * [UserNotFoundException] if the user was not found
     */
    fun findUser(id: String): CompletableFuture<User>

    /**
     * Finds the given [User] for the list of [id]s
     */
    fun findUsers(id: Iterable<String>): CompletableFuture<List<User>>

    data class User(val id: String, val username: String, val discriminator: String) {
        constructor(user: net.dv8tion.jda.api.entities.User) : this(user.id, user.name,
                user.discriminator)

        val nameAndDiscriminator: String = "${username}#${discriminator}"
    }

    class UserNotFoundException : Exception("The requested user was not found")
}