package me.mrkirby153.KirBot.modules

import co.aikar.idb.DB
import co.aikar.idb.Database
import co.aikar.idb.DatabaseOptions
import co.aikar.idb.PooledDatabaseOptions
import me.mrkirby153.KirBot.database.DatabaseConnection
import me.mrkirby153.KirBot.database.models.ConnectionFactory
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.module.Module
import java.sql.Connection

class Database : Module("database") {

    @Deprecated("Deprecated")
    lateinit var database: DatabaseConnection

    lateinit var db: Database

    override fun onLoad() {
        val host = getProp("database-host") ?: "localhost"
        val port = getProp("database-port")?.toInt() ?: 3306
        val database = getProp("database") ?: "KirBot"

        val username = getProp("database-username") ?: "root"
        val password = getProp("database-password") ?: ""
        log("Connecting to database $database at $host:$port ($username)")

//        this.database = DatabaseConnection(host, port, database, username, password)
        val options = DatabaseOptions.builder().mysql(username, password, database,
                "$host:$port").poolName("db-pool").build()
        this.db = PooledDatabaseOptions.builder().options(options).createHikariDatabase()
        DB.setGlobalDatabase(this.db)
        Model.factory = object : ConnectionFactory {
            override fun getConnection(): Connection {
                return this@Database.db.connection
            }
        }
    }
}