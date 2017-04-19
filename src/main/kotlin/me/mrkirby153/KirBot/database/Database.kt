package me.mrkirby153.KirBot.database

import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.Member
import java.sql.Connection
import java.sql.DriverManager

object Database {

    var connection: Connection

    init {
        val host = Bot.properties.getProperty("mysql-host")
        val port = Bot.properties.getProperty("mysql-port")
        val username = Bot.properties.getProperty("mysql-username")
        val password = Bot.properties.getProperty("mysql-password")
        val database = Bot.properties.getProperty("mysql-database")
        connection = DriverManager.getConnection("jdbc:mysql://$host:$port/$database", username, password)
    }

    fun getRealname(firstNameOnly: Boolean, member: Member): String? {
        val sql = if (firstNameOnly) "SELECT `first_name` AS `name` FROM `user_info` WHERE `id` = ?" else
            "SELECT CONCAT(`first_name`, ' ', `last_name`) AS `name` FROM `user_info` WHERE `id` = ?"
        val ps = connection.prepareStatement(sql)
        ps.setString(1, member.user.id)
        val rs = ps.executeQuery()
        if (rs.next())
            return rs.getString("name")
        else
            return null
    }

}