package com.mrkirby153.kirbot.services.impl

import com.mrkirby153.kirbot.entity.repo.DiscordUserRepository
import com.mrkirby153.kirbot.services.UserService
import net.dv8tion.jda.api.sharding.ShardManager
import org.apache.logging.log4j.LogManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

class UserManager(private val seenUserRepository: DiscordUserRepository,
                  private val shardManager: ShardManager) : UserService {

    private val log = LogManager.getLogger()

    override fun findUser(id: String): CompletableFuture<UserService.User> {
        val future = CompletableFuture<UserService.User>()
        shardManager.retrieveUserById(id).queue({
            future.complete(UserService.User(it))
        }, {
            val seenUser = seenUserRepository.findById(id)
            seenUser.ifPresentOrElse({
                future.complete(UserService.User(it.id!!, it.username, it.discriminator.toString()))
            }, {
                future.completeExceptionally(UserService.UserNotFoundException())
            })
        })
        return future
    }

    override fun findUsers(id: Iterable<String>): CompletableFuture<List<UserService.User>> {
        val futures = id.map { findUser(it) }
        return CompletableFuture.allOf(*futures.toTypedArray()).thenApply {
            futures.mapNotNull {
                try {
                    it.join()
                } catch (e: ExecutionException) {
                    log.debug("Could not retrieve user", e)
                    null
                }
            }
        }
    }

}