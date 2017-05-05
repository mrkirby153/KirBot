package me.mrkirby153.KirBot.database

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.realname.RealnameSetting
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp

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

    fun onJoin(server: Server) {
        Database.updateChannels(server)
        if (!serverExists(server)) {
            val ps = connection.prepareStatement("INSERT INTO `server_settings` (`id`, `name`, `require_realname`, `realname`, `created_at`, `updated_at`) VALUES(?, ?, '0', 'OFF', ?, ?)")
            ps.setString(1, server.id)
            ps.setString(2, server.name)
            ps.setTimestamp(3, Timestamp(System.currentTimeMillis()))
            ps.setTimestamp(4, Timestamp(System.currentTimeMillis()))
            ps.executeUpdate()
        } else {
            val ps = connection.prepareStatement("UPDATE `server_settings` SET `name` = ? WHERE `id` = ?")
            ps.setString(1, server.name)
            ps.setString(2, server.id)
            ps.executeUpdate()
        }
    }

    fun onLeave(server: Server) {
        val ps = connection.prepareStatement("DELETE FROM `server_settings` WHERE `id` = ?")
        ps.setString(1, server.id)
        ps.executeUpdate()
        removeChannel(server)
    }

    fun serverExists(server: Server): Boolean {
        val ps = connection.prepareStatement("SELECT COUNT(*) AS `count` FROM `server_settings` WHERE `id` = ?")
        ps.setString(1, server.id)
        val rs = ps.executeQuery()
        if (!rs.next())
            return false
        else
            return rs.getInt("count") > 0
    }

    fun addChannel(channel: Channel) {
        val ps = connection.prepareStatement("INSERT INTO `channels` (`id`, `server`, `channel_name`, `type`, `created_at`, `updated_at`) VALUES (?, ?, ?, ?, ?, ?)")
        ps.setString(1, channel.id)
        ps.setString(2, channel.guild.id)
        ps.setString(3, channel.name)
        ps.setString(4, if (channel is TextChannel) "TEXT" else "VOICE")
        ps.setTimestamp(5, Timestamp(System.currentTimeMillis()))
        ps.setTimestamp(6, Timestamp(System.currentTimeMillis()))

        ps.executeUpdate()
    }

    fun updateChannel(channel: Channel) {
        val ps = connection.prepareStatement("UPDATE `channels` SET `channel_name` = ? WHERE `id` = ?")
        ps.setString(1, channel.name)
        ps.setString(2, channel.id)
        ps.executeUpdate()
    }

    fun getChannels(server: Server): MutableList<String> {
        val chans = mutableListOf<String>()
        val ps = connection.prepareStatement("SELECT `id` FROM `channels` WHERE `server` = ?")
        ps.setString(1, server.id)
        val rs = ps.executeQuery()
        while (rs.next()) {
            chans.add(rs.getString("id"))
        }
        return chans
    }

    fun removeChannel(server: Server) {
        val ps = connection.prepareStatement("DELETE FROM `channels` WHERE `server` = ?")
        ps.setString(1, server.id)
        ps.executeUpdate()
    }

    fun removeChannel(channel: String) {
        val ps = connection.prepareStatement("DELETE FROM `channels` WHERE `id` = ?")
        ps.setString(1, channel)
        ps.executeUpdate()
    }

    fun updateChannels(server: Server) {
        val chans = getChannels(server)
        val c = mutableListOf<Channel>()
        c.addAll(server.textChannels)
        c.addAll(server.voiceChannels)
        for (channel in c) {
            if (channel.id in chans) {
                updateChannel(channel)
            } else {
                addChannel(channel)
            }
            chans.remove(channel.id)
        }
        chans.forEach {
            removeChannel(it)
            Bot.LOG.info("Removing channel $it")
        }
    }

    fun getLoggingChannel(server: Server): String? {
        val ps = connection.prepareStatement("SELECT `log_channel` FROM `server_settings` WHERE `id` = ?")
        ps.setString(1, server.id)
        val rs = ps.executeQuery()
        if (rs.next()) {
            return rs.getString("log_channel")
        } else {
            return null
        }
    }
}