package me.mrkirby153.KirBot.database.models.guild

import me.mrkirby153.KirBot.database.models.AutoIncrementing
import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.JsonArray
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.PrimaryKey
import me.mrkirby153.KirBot.database.models.Table

@Table("music_settings")
@AutoIncrementing(false)
class MusicSettings : Model() {

    @PrimaryKey
    var id = ""

    var enabled = false

    @Column("mode")
    private var modeRaw = "OFF"

    @Column("channels")
    @JsonArray
    var channels: List<String> = emptyList()

    @Column("max_queue_length")
    var maxQueueLength = -1

    var playlists = true

    @Column("max_song_length")
    var maxSongLength = -1

    @Column("skip_cooldown")
    var skipCooldown = 0

    @Column("skip_timer")
    var skipTimer = 0

    override fun toString(): String {
        return "MusicSettings(id='$id', enabled=$enabled, modeRaw='$modeRaw', channels=$channels, maxQueueLength=$maxQueueLength, playlists=$playlists, maxSongLength=$maxSongLength, skipCooldown=$skipCooldown, skipTimer=$skipTimer)"
    }


}