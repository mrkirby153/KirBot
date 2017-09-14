package me.mrkirby153.KirBot.music_legacy


class MusicData(val server: Long, val enabled: Boolean = true, val whitelistMode: WhitelistMode = MusicData.WhitelistMode.OFF,
                channels: String = "", blacklistedSongs: String = "", val maxQueueLength: Int = -1, val maxSongLength: Int = -1,
                val skipCooldown: Int = 0, val skipTimer: Int = 30, val playlists: Boolean = true) {

    val channels = channels.split(",").map { it.trim() }

    val blacklistedSongs = blacklistedSongs.split(",").map { it.trim() }

    enum class WhitelistMode {
        OFF,
        WHITELIST,
        BLACKLIST
    }
}