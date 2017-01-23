package me.mrkirby153.KirBot.net

interface NetworkMessageHandler {
    fun handle(message: NetworkMessage)
}