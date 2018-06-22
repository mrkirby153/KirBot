package me.mrkirby153.KirBot.database.models.guild

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model

@Table("command_aliases")
class CommandAlias : Model() {

    var id: String = ""

    @Column("server_id")
    var serverId: String = ""

    var command: String = ""
        get() = field.toLowerCase()

    var alias: String? = ""

    var clearance: Int = 0
}