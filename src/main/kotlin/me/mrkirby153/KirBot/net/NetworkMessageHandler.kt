package me.mrkirby153.KirBot.net

interface NetworkMessageHandler {

    val requireAuth: Boolean

    fun handle(message: NetworkMessage)
}