package me.mrkirby153.KirBot.database

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.music.MusicData
import me.mrkirby153.KirBot.realname.RealnameSetting
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.use
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Guild
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
        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, member.user.id)
            ps.executeQuery().use {
                if (it.next())
                    return it.getString("name")
                else
                    return null
            }
        }
    }


    fun getCustomCommand(cmd: String, server: Guild): DBCommand? {
        val sql = "SELECT `id`, `server`, `name`, `data`, `clearance`, `type` FROM `custom_commands` WHERE `server` = ? AND `name` = ?"
        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, server.id)
            ps.setString(2, cmd)
            ps.executeQuery().use { rs ->
                if (rs.next())
                    return DBCommand(rs.getString("id"), rs.getString("name"), server, rs.getString("data"),
                            Clearance.valueOf(rs.getString("clearance")), CommandType.valueOf(rs.getString("type")))
                else
                    return null
            }
        }
    }

    fun getRealnameSetting(server: Guild): RealnameSetting? {
        connection.prepareStatement("SELECT `realname` FROM `server_settings` WHERE `id` = ?").use { ps ->
            ps.setString(1, server.id)
            ps.executeQuery().use { rs ->
                if (rs.next()) {
                    return RealnameSetting.valueOf(rs.getString("realname"))
                }
                return null
            }
        }
    }

    fun requireRealname(server: Guild): Boolean {
        connection.prepareStatement("SELECT `require_realname` FROM `server_settings` WHERE `id` = ?").use { ps ->
            ps.setString(1, server.id)
            ps.executeQuery().use { rs ->
                if (rs.next())
                    return rs.getBoolean("require_realname")
                return false
            }
        }
    }

    fun getCommandPrefix(server: Guild): String {
        connection.prepareStatement("SELECT `command_discriminator` FROM `server_settings` WHERE `id` = ?").use { ps ->
            ps.setString(1, server.id)
            ps.executeQuery().use { rs ->
                if (rs.next())
                    return rs.getString("command_discriminator")
                return "!"
            }
        }

    }

    fun onJoin(server: Guild) {
        Database.updateChannels(server)
        if (!serverExists(server)) {
            connection.prepareStatement("INSERT INTO `server_settings` (`id`, `name`, `require_realname`, `realname`, `created_at`, `updated_at`) VALUES(?, ?, '0', 'OFF', ?, ?)").use { ps ->
                ps.setString(1, server.id)
                ps.setString(2, server.name)
                ps.setTimestamp(3, Timestamp(System.currentTimeMillis()))
                ps.setTimestamp(4, Timestamp(System.currentTimeMillis()))
                ps.executeUpdate()
            }
        } else {
            connection.prepareStatement("UPDATE `server_settings` SET `name` = ? WHERE `id` = ?").use { ps ->
                ps.setString(1, server.name)
                ps.setString(2, server.id)
                ps.executeUpdate()
            }

        }
    }

    fun onLeave(server: Guild) {
        connection.prepareStatement("DELETE FROM `server_settings` WHERE `id` = ?").use { ps ->
            ps.setString(1, server.id)
            ps.executeUpdate()
        }
        removeChannel(server)
    }

    fun serverExists(server: Guild): Boolean {
        connection.prepareStatement("SELECT COUNT(*) AS `count` FROM `server_settings` WHERE `id` = ?").use { ps ->
            ps.setString(1, server.id)
            ps.executeQuery().use { rs ->
                if (!rs.next())
                    return false
                else
                    return rs.getInt("count") > 0
            }
        }
    }

    fun addChannel(channel: Channel) {
        connection.prepareStatement("INSERT INTO `channels` (`id`, `server`, `channel_name`, `type`, `created_at`, `updated_at`) VALUES (?, ?, ?, ?, ?, ?)").use { ps ->
            ps.setString(1, channel.id)
            ps.setString(2, channel.guild.id)
            ps.setString(3, channel.name)
            ps.setString(4, if (channel is TextChannel) "TEXT" else "VOICE")
            ps.setTimestamp(5, Timestamp(System.currentTimeMillis()))
            ps.setTimestamp(6, Timestamp(System.currentTimeMillis()))

            ps.executeUpdate()
        }
    }

    fun updateChannel(channel: Channel) {
        connection.prepareStatement("UPDATE `channels` SET `channel_name` = ? WHERE `id` = ?").use { ps ->
            ps.setString(1, channel.name)
            ps.setString(2, channel.id)
            ps.executeUpdate()
        }
    }

    fun getChannels(server: Guild): MutableList<String> {
        val chans = mutableListOf<String>()
        connection.prepareStatement("SELECT `id` FROM `channels` WHERE `server` = ?").use { ps ->
            ps.setString(1, server.id)
            ps.executeQuery().use { rs->
                while (rs.next()) {
                    chans.add(rs.getString("id"))
                }
            }
        }
        return chans
    }

    fun removeChannel(server: Guild) {
        connection.prepareStatement("DELETE FROM `channels` WHERE `server` = ?").use { ps ->
            ps.setString(1, server.id)
            ps.executeUpdate()
        }
    }

    fun removeChannel(channel: String) {
        connection.prepareStatement("DELETE FROM `channels` WHERE `id` = ?").use { ps ->
            ps.setString(1, channel)
            ps.executeUpdate()
        }
    }

    fun updateChannels(server: Guild) {
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
            Bot.LOG.info("Removing context $it")
        }
    }

    fun getLoggingChannel(server: Guild): String? {
        connection.prepareStatement("SELECT `log_channel` FROM `server_settings` WHERE `id` = ?").use { ps ->
            ps.setString(1, server.id)
             ps.executeQuery().use { rs ->
                if (rs.next()) {
                    return rs.getString("log_channel")
                } else {
                    return null
                }
            }

        }
    }

    fun getMusicData(server: Guild): MusicData {
         connection.prepareStatement("SELECT * FROM `music_settings` WHERE `id` = ?").use { ps ->
            ps.setString(1, server.id)
            ps.executeQuery().use { rs ->
                if (rs.next()) {
                    return MusicData(server.idLong, rs.getBoolean("enabled"), MusicData.WhitelistMode.valueOf(rs.getString("mode")),
                            rs.getString("channels"), rs.getString("blacklist_songs"), rs.getInt("max_queue_length"), rs.getInt("max_song_length"),
                            rs.getInt("skip_cooldown"), rs.getInt("skip_timer"), rs.getBoolean("playlists"))
                } else {
                    connection.prepareStatement("INSERT INTO `music_settings` (`id`, `channels`, `blacklist_songs`, `created_at`, `updated_at`) VALUES(?, '', '', ?, ?)").apply {
                        setString(1, server.id)
                        setTimestamp(2, Timestamp(System.currentTimeMillis()))
                        setTimestamp(3, Timestamp(System.currentTimeMillis()))
                    }.executeUpdate()
                    return MusicData(server.idLong)
                }
            }
        }
    }
}