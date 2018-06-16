package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.utils.toTypedArray
import org.json.JSONArray
import org.json.JSONTokener

@Table("music_settings")
class MusicSettings : Model() {

    init {
        this.incrementing = false
    }

    @PrimaryKey
    var id = ""

    var enabled = true

    @Column("mode")
    private var modeRaw = "OFF"

    @Column("channels")
    var channelsRaw: String = ""

    var channels: List<String>
        get() = JSONArray(JSONTokener(this.channelsRaw)).toTypedArray(String::class.java)
        set(value) {
            channelsRaw = value.toString()
        }


    @Column("max_queue_length")
    var maxQueueLength = -1

    var playlists = true

    @Column("max_song_length")
    var maxSongLength = -1

    @Column("skip_cooldown")
    var skipCooldown = 0

    @Column("skip_timer")
    var skipTimer = 30

    override fun toString(): String {
        return "MusicSettings(id='$id', enabled=$enabled, modeRaw='$modeRaw', channels=$channels, maxQueueLength=$maxQueueLength, playlists=$playlists, maxSongLength=$maxSongLength, skipCooldown=$skipCooldown, skipTimer=$skipTimer)"
    }

    @Transient
    var whitelistMode: WhitelistMode = WhitelistMode.OFF
        get() = WhitelistMode.valueOf(modeRaw)
        set(mode) {
            modeRaw = mode.toString()
            field = mode
        }

    enum class WhitelistMode {
        WHITELIST,
        BLACKLIST,
        OFF
    }

}