package me.mrkirby153.KirBot.database.models

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.utils.isNumber

@Table("guild_settings")
class GuildSetting : Model() {

    @Transient
    private val numInt = Regex("\"(\\d+)\"")

    @Transient
    // The maximum safe value for an integer in JS
    private val maxSafe = 9007199254740991

    var id = ""

    @Column("guild")
    var guildId = ""

    var key = ""

    @Column("value")
    private var valueRaw: String? = ""

    var value: String?
        get() {
            val mr = numInt.matchEntire(this.valueRaw ?: "")
            return if (mr != null) {
                mr.groups[1]!!.value
            } else {
                this.valueRaw
            }
        }
        set(value) {
            if (value == null) {
                valueRaw = null
                return
            }
            if (value.isNumber()) {
                if (value.toDouble() > maxSafe.toDouble()) {
                    valueRaw = "\"$value\""
                    return
                }
            }
            valueRaw = value
        }

    init {
        this.incrementing = false
    }

    override fun create() {
        id = "${guildId}_$key"
        super.create()
    }
}