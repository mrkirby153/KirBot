package me.mrkirby153.KirBot.infraction

enum class InfractionType(val internalName: String) {

    WARNING("warning"),
    KICK("kick"),
    BAN("ban"),
    MUTE("mute"),
    TEMPMUTE("tempmute"),
    UNKNOWN("unknown");

    companion object {
        fun getType(internalName: String): InfractionType {
            return InfractionType.values().firstOrNull { it.internalName == internalName } ?: UNKNOWN
        }
    }
}