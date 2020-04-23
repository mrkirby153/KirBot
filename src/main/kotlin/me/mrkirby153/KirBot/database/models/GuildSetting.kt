package me.mrkirby153.KirBot.database.models


import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.model.annotations.Column
import com.mrkirby153.bfs.model.annotations.Table
import com.mrkirby153.bfs.model.annotations.Timestamps
import com.mrkirby153.bfs.model.enhancers.TimestampEnhancer
import me.mrkirby153.KirBot.utils.isNumber
import java.sql.Timestamp

@Table("guild_settings")
@Timestamps
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

    @TimestampEnhancer.CreatedAt
    @Column("created_at")
    var createdAt: Timestamp? = null

    @TimestampEnhancer.UpdatedAt
    @Column("updated_at")
    var updatedAt: Timestamp? = null


    override fun create() {
        id = "${guildId}_$key"
        super.create()
    }
}