package me.mrkirby153.KirBot

object Main {

    @JvmStatic fun main(args: Array<String>) {
        Bot.start(Bot.properties.getProperty("auth-token"))
    }
}
