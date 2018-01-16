package me.mrkirby153.KirBot.database.models

import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Guild

@Table("channels")
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

    val type: Type
        get() = Type.valueOf(this.typeRaw)

    val guild: Guild?
        get() = Bot.shardManager.getGuild(this.guildId)

    val channel: Channel?
        get() = guild?.getTextChannelById(this.id) as? Channel ?: guild?.getVoiceChannelById(
                this.id) as? Channel


    enum class Type {
        VOICE,
        TEXT
    }
}