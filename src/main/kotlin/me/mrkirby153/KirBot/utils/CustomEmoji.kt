package me.mrkirby153.KirBot.utils

class CustomEmoji(val name: String, val id: String) {

    override fun toString(): String {
        return "<:$name:$id>"
    }
}