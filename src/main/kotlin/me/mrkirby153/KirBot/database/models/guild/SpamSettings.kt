package me.mrkirby153.KirBot.database.models.guild

import me.mrkirby153.KirBot.database.models.Column
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.Table
import org.json.JSONObject
import org.json.JSONTokener

@Table("spam_settings")
class SpamSettings : Model() {

    var id: String = ""

    @Column("settings")
    private var rawSettings: String = "{}"


    val settings: JSONObject
        get() = JSONObject(JSONTokener(this.rawSettings))
}