package me.mrkirby153.KirBot.database.models

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.sharding.ShardManager

@Table("channels")
class Channel(channel: GuildChannel? = null) : Model() {

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

    var type: Type
        get() = Type.valueOf(this.typeRaw)
        set(value) {
            this.typeRaw = value.toString()
        }

    var guild: Guild?
        get() = Bot.applicationContext.get(ShardManager::class.java).getGuildById(this.guildId)
        set(guild) {
            this.guildId = guild!!.id
        }

    var channel: GuildChannel?
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
        }

    init {
        this.incrementing = false
        if(channel != null){
            this.id = channel.id
            this.guildId = channel.guild.id
            this.name = channel.name
            this.type = getType(channel)
            if(guild != null) {
                this.hidden = channel.getPermissionOverride(guild!!.publicRole)?.denied?.contains(Permission.MESSAGE_WRITE) ?: false
            } else {
                this.hidden = false
            }
        }
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
        if(guild != null) {
            this.hidden = channel.getPermissionOverride(guild!!.publicRole)?.denied?.contains(
                    Permission.MESSAGE_READ) ?: false
        } else {
            this.hidden = false
        }
        save()
    }

    fun getType(channel: GuildChannel?): Type {
        if (channel == null)
            return Type.UNKNOWN
        if (channel is TextChannel)
            return Type.TEXT
        if (channel is VoiceChannel)
            return Type.VOICE
        return Type.UNKNOWN
    }

}