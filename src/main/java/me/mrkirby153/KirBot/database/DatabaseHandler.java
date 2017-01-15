package me.mrkirby153.KirBot.database;

import me.mrkirby153.KirBot.KirBot;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Handles interaction with the database
 */
public class DatabaseHandler {

    /**
     * The database connection string
     */
    private final String connectionString;

    /**
     * The database connection
     */
    private Connection connection;

    /**
     * The jOOQ database context
     */
    private DSLContext create;


    public DatabaseHandler(KirBot kirBot, String host, int port, String username, String password, String database) {
        connectionString = String.format("jdbc:mysql://%s:%s/%s", host, port, database);
        try {
            connection = DriverManager.getConnection(connectionString, username, password);
            create = DSL.using(connection, SQLDialect.MYSQL);
        } catch (SQLException e) {
            KirBot.logger.error("There was an error connecting to the database!", e);
            kirBot.shutdown("Could not connect to database", -4);
        }
    }

    /**
     * Gets the connection string/URL
     *
     * @return The connection string
     */
    public String getConnectionString() {
        return connectionString;
    }

    /**
     * Gets the connection to the database
     *
     * @return The connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Gets the {@link DSLContext}
     *
     * @return The DSLContext
     */
    public DSLContext create() {
        return create;
    }

    /**
     * Closes the MySQL database connections
     */
    public void close() {
        create.close();
        try {
            connection.close();
        } catch (SQLException e) {
            KirBot.logger.error("There was an error disconencting from the database", e);
        }
    }
}
