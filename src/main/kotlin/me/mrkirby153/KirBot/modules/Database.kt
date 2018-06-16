package me.mrkirby153.KirBot.modules

import com.mrkirby153.bfs.ConnectionFactory
import com.mrkirby153.bfs.sql.QueryBuilder
import me.mrkirby153.KirBot.database.DatabaseConnection
import me.mrkirby153.KirBot.module.Module
import java.sql.Connection

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
        QueryBuilder.connectionFactory = object: ConnectionFactory {
            override fun getConnection(): Connection {
                return this@Database.database.getConnection()
            }

        }
    }
}