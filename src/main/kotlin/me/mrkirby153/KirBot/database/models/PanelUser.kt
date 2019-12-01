package me.mrkirby153.KirBot.database.models

import com.mrkirby153.bfs.annotations.Column
import com.mrkirby153.bfs.annotations.Table
import com.mrkirby153.bfs.model.Model
import java.sql.Timestamp

@Table("users")
class PanelUser : Model() {

    var id: String = ""

    var username: String = ""

    var admin: Boolean = false

    var token: String = ""

    @Column("refresh_token")
    var refreshToken: String = ""

    @Column("expires_in")
    var expiresIn: Timestamp? = null

    @Column("remember_token")
    var rememberToken: String = ""

    @Column("api_token")
    var apiToken: String = ""
}