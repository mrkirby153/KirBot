package me.mrkirby153.KirBot.database

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.mrkirby153.KirBot.Bot
import java.sql.Connection
import java.util.concurrent.Executors

class DatabaseConnection(host: String, port: Int, database: String,
                         username: String, password: String) {

    private val dataSource: HikariDataSource

    internal val executorPool = Executors.newFixedThreadPool(5,
            ThreadFactoryBuilder().setNameFormat("Database Connection-%d").setDaemon(true).build())

    init {
        val cfg = HikariConfig()
        cfg.jdbcUrl = "jdbc:mysql://$host:$port/$database?useSSL=false"
        cfg.username = username
        cfg.password = password
        cfg.leakDetectionThreshold = if(Bot.debug) 2000 else 0

        Bot.LOG.debug("Connecting to ${cfg.jdbcUrl}")

        // Optimizations for the connections
        cfg.addDataSourceProperty("cachePrepStmts", true)
        cfg.addDataSourceProperty("prepStmtCacheSize", 250)
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", 2048)
        cfg.addDataSourceProperty("useServerPrepStmts", true)
        cfg.addDataSourceProperty("useLocalSessionState", true)
        cfg.addDataSourceProperty("rewriteBatchStatements", true)
        cfg.addDataSourceProperty("cacheResultSetMetadata", true)
        cfg.addDataSourceProperty("cacheServerConfiguration", true)

        dataSource = HikariDataSource(cfg)
    }

    fun getConnection(): Connection = this.dataSource.connection
}