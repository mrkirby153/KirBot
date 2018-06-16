package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
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