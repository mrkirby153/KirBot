package me.mrkirby153.KirBot.database.models

import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Guild

@Table("channels")
@AutoIncrementing(false)
class Channel : Model() {

    @PrimaryKey
    var id = ""

    @Column("server")
    private var guildId = ""

    @Column("channel_name")
    var name = ""

    @Column("type")
    private var typeRaw = ""

    @Column("hidden")
    var hidden = false

    @Transient
    var type: Type = Type.UNKNOWN
        get() = Type.valueOf(this.typeRaw)
        set(value) {
            this.typeRaw = value.toString()
            field = value
        }

    @Transient
    var guild: Guild? = null
        get() = Bot.shardManager.getGuild(this.guildId)
        set(guild) {
            this.guildId = guild!!.id
            field = guild
        }

    @Transient
    var channel: Channel? = null
        get() = guild?.getTextChannelById(this.id) as? Channel ?: guild?.getVoiceChannelById(
                this.id) as? Channel
        set(channel) {
            this.id = channel!!.id
            field = channel
        }


    enum class Type {
        VOICE,
        TEXT,
        UNKNOWN
    }

    override fun toString(): String {
        return "Channel(id='$id', guildId='$guildId', name='$name', typeRaw='$typeRaw', hidden=$hidden)"
    }


}