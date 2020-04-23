package me.mrkirby153.KirBot.command.executors.moderation

import com.mrkirby153.bfs.query.DB
import com.mrkirby153.bfs.query.DbRow
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.convertSnowflake
import me.mrkirby153.KirBot.utils.uploadToArchive
import java.text.SimpleDateFormat


class CommandArchive{

    @Command(name = "user", clearance = CLEARANCE_MOD,
            arguments = ["<user:snowflake>", "[amount:int]"], category = CommandCategory.MODERATION, parent = "archive")
    @CommandDescription("Create an archive of messages that a user has sent")
    @IgnoreWhitelist
    fun archiveUser(context: Context, cmdContext: CommandContext) {
        val amount = cmdContext.get<Int>("amount") ?: 50
        val user = cmdContext.get<String>("user")!!
        val messages = DB.raw(
                "SELECT * FROM server_messages WHERE author = ? AND server_id = ? ORDER BY id DESC LIMIT ?", user,
                context.guild.id, amount).map { decode(it) }.reversed()
        val archiveUrl = createArchive(messages)
        context.channel.sendMessage("Archived ${messages.count()} messages at $archiveUrl").queue()
    }

    @Command(name = "channel", clearance = CLEARANCE_MOD,
            arguments = ["<channel:snowflake>", "[amount:int]"], category = CommandCategory.MODERATION, parent = "archive")
    @CommandDescription("Creates an archive of messages sent in a channel")
    @IgnoreWhitelist
    fun archiveChannel(context: Context, cmdContext: CommandContext) {
        val amount = cmdContext.get<Int>("amount") ?: 50
        val chan = cmdContext.get<String>("channel")!!
        if(context.guild.getTextChannelById(chan) == null)
            throw CommandException("That channel doesn't exist")
        val messages = DB.raw(
                "SELECT * FROM server_messages WHERE channel = ? ORDER BY id DESC LIMIT ?", chan,
                amount).map { decode(it) }.reversed()
        val archiveUrl = createArchive(messages)
        context.channel.sendMessage("Archived ${messages.count()} messages at $archiveUrl").queue()
    }


    private fun createArchive(messages: Collection<GuildMessage>): String {
        val msgs = mutableListOf<String>()
        val userIds = messages.map { it.author }.toSet()
        val userMap = mutableMapOf<String, String>()
        if (userIds.isNotEmpty()) {
            val results = DB.raw(
                    "SELECT id, username, discriminator FROM seen_users WHERE id IN (${userIds.joinToString(
                            ", ")})")
            results.forEach {
                userMap[it.getString("id")] = "${it.getString("username")}#${it.getInt(
                        "discriminator")}"
            }
        }
        messages.forEach {
            val timeFormatted = SimpleDateFormat("YYY-MM-dd HH:MM:ss").format(
                    convertSnowflake(it.id))
            msgs.add(String.format("%s (%s / %s / %s) %s: %s (%s)", timeFormatted, it.serverId,
                    it.channel, it.author, userMap[it.author] ?: it.author, it.message, it.attachments ?: ""))
        }

        return uploadToArchive(LogManager.encrypt(msgs.joinToString("\n")))
    }

    private fun decode(row: DbRow): GuildMessage {
        val msg = GuildMessage()
        msg.hydrate(row)
        return msg
    }
}