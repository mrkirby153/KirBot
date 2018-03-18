package me.mrkirby153.KirBot.database.models.guild

import me.mrkirby153.KirBot.database.models.AutoIncrementing
import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.JsonArray
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.PrimaryKey
import me.mrkirby153.KirBot.database.models.Table
import me.mrkirby153.KirBot.realname.RealnameSetting

@Table("server_settings")
@AutoIncrementing(false)
class ServerSettings : Model() {

    @PrimaryKey
    var id = ""

    var name = ""

    @Column("realname")
    private var realnameRaw = ""

    @Column("require_realname")
    var requireRealname = false

    @Column("command_discriminator")
    var cmdDiscriminator = "!"

    @Column("log_channel")
    var logChannel: String? = null

    @Column("cmd_whitelist")
    @JsonArray
    var cmdWhitelist = mutableListOf<String>()

    @Column("bot_nick")
    var botNick: String? = null

    @Column("user_persistence")
    var persistence = false

    @Column("log_timezone")
    var logTimezone = "UTC"

    @Transient
    var realname: RealnameSetting = RealnameSetting.OFF
        get() = RealnameSetting.valueOf(realnameRaw)
        set(setting) {
            this.realnameRaw = setting.toString()
            field = setting
        }
}