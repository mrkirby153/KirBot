package me.mrkirby153.KirBot

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import me.mrkirby153.KirBot.inject.ApplicationContext
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Named

fun main() {

    (LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as? Logger)?.level = Level.TRACE

    val context = ApplicationContext()

    context.register(A::class.java)
    context.register("secret key", "bot_key")

    context.newInstance(B::class.java).testing()

}

class A {

    @Inject
    @Named("bot_key")
    val value = "A"
}

class C {

}

class B @Inject constructor(val a: A) {

    fun testing() {
        println(a.value)
    }
}