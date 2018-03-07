package me.mrkirby153.KirBot.database.models

import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel

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
        get() {
            val guild = guild ?: return null
            return when (type) {
                Type.VOICE -> guild.getVoiceChannelById(id)
                Type.TEXT -> guild.getTextChannelById(id)
                else -> null
            }
        }
        set(channel) {
            this.id = channel!!.id
            this.type = getType(channel)
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

    fun updateChannel() {
        val channel = this.channel ?: return
        this.name = channel.name
        this.type = getType(channel)
        this.hidden = channel.getPermissionOverride(guild?.publicRole)?.denied?.contains(
                Permission.MESSAGE_READ) ?: false
        save()
    }

    fun getType(channel: Channel?): Type {
        if (channel == null)
            return Type.UNKNOWN
        if (channel is TextChannel)
            return Type.TEXT
        if (channel is VoiceChannel)
            return Type.VOICE
        return Type.UNKNOWN
    }

}