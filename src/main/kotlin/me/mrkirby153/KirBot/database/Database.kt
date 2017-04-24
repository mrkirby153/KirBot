package me.mrkirby153.KirBot.database

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.realname.RealnameSetting
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
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

    fun getCustomCommand(cmd: String, server: Server): DBCommand? {
        val ps = connection.prepareStatement("SELECT `id`, `server`, `name`, `data`, `clearance`, `type` FROM `custom_commands` WHERE `server` = ? AND `name` = ?")
        ps.setString(1, server.id)
        ps.setString(2, cmd)

        val rs = ps.executeQuery()

        if (rs.next())
            return DBCommand(rs.getString("id"), rs.getString("name"), server, rs.getString("data"),
                    Clearance.valueOf(rs.getString("clearance")), CommandType.valueOf(rs.getString("type")))
        else
            return null
    }

    fun getCustomCommands(server: Server): List<DBCommand> {
        val cmds = mutableListOf<DBCommand>()

        val ps = connection.prepareStatement("SELECT `id`, `server`, `name`, `data`, `clearance`, `type` FROM `custom_commands` WHERE `server` = ?")
        ps.setString(1, server.id)

        val rs = ps.executeQuery()
        while (rs.next()) {
            cmds.add(DBCommand(rs.getString("id"), rs.getString("name"), server, rs.getString("data"),
                    Clearance.valueOf(rs.getString("clearance")), CommandType.valueOf(rs.getString("type"))))
        }
        return cmds
    }

    fun getRealnameSetting(server: Server): RealnameSetting? {
        val ps = connection.prepareStatement("SELECT `realname` FROM `server_settings` WHERE `id` = ?")
        ps.setString(1, server.id)

        val rs = ps.executeQuery()
        if (rs.next()) {
            return RealnameSetting.valueOf(rs.getString("realname"))
        }
        return null
    }

    fun requireRealname(server: Server): Boolean {
        val ps = connection.prepareStatement("SELECT `require_realname` FROM `server_settings` WHERE `id` = ?")
        ps.setString(1, server.id)

        val rs = ps.executeQuery()
        if (rs.next())
            return rs.getBoolean("require_realname")
        return false
    }

    fun getCommandPrefix(server: Server): String {
        val ps = connection.prepareStatement("SELECT `command_discriminator` FROM `server_settings` WHERE `id` = ?")
        ps.setString(1, server.id)
        val rs = ps.executeQuery()
        if (rs.next())
            return rs.getString("command_discriminator")
        return "!"
    }
}