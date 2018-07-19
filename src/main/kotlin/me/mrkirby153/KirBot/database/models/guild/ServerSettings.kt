package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.PrimaryKey
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.realname.RealnameSetting
import me.mrkirby153.KirBot.utils.toTypedArray
import org.json.JSONArray
import org.json.JSONTokener

@Table("server_settings")
class ServerSettings : Model() {

    init {
        this.incrementing = false
    }

    @PrimaryKey
    var id = ""

    var name = ""

    @Column("realname")
    private var realnameRaw = ""

    @Column("require_realname")
    var requireRealname = false

    @Column("command_discriminator")
    var cmdDiscriminator = "!"

    @Column("cmd_whitelist")
    var cmdWhitelistRaw = ""

    @Column("icon_id")
    var iconId = ""

    var cmdWhitelist: List<String>
        get() = JSONArray(JSONTokener(this.cmdWhitelistRaw)).toTypedArray(String::class.java)
        set(value) {
            this.cmdWhitelistRaw = JSONArray().apply {
                value.forEach {
                    put(it)
                }
            }.toString()
        }

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