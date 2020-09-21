package com.mrkirby153.kirbot.utils

import io.mockk.every
import io.mockk.mockk
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

/**
 * Mocks a completed future and calls any callbacks with the given [result]
 */
inline fun <reified T> mockCompletedFuture(result: T): CompletableFuture<T> {
    val future = mockk<CompletableFuture<T>>()
    every { future.thenAccept(any<Consumer<T>>()) }.answers {
        (it.invocation.args[0] as? Consumer<T>)?.accept(result)
        CompletableFuture.completedFuture(null)
    }
    return future
}