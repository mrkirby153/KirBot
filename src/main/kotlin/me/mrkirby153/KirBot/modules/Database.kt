package me.mrkirby153.KirBot.modules

import com.mrkirby153.bfs.connection.ConnectionFactory
import com.mrkirby153.bfs.query.QueryBuilder
import me.mrkirby153.KirBot.database.DatabaseConnection
import me.mrkirby153.KirBot.module.Module

class Database : Module("database") {

    lateinit var database: DatabaseConnection

    lateinit var db: Database

    override fun onLoad() {
        val host = getProp("database-host") ?: "localhost"
        val port = getProp("database-port")?.toInt() ?: 3306
        val database = getProp("database") ?: "KirBot"

        val username = getProp("database-username") ?: "root"
        val password = getProp("database-password") ?: ""
        log("Connecting to database $database at $host:$port ($username)")

        this.database = DatabaseConnection(host, port, database, username, password)
        QueryBuilder.defaultConnectionFactory = ConnectionFactory { this@Database.database.getConnection() }
    }
}