package me.mrkirby153.KirBot.command.executors.moderation

import com.mrkirby153.bfs.sql.DB
import com.mrkirby153.bfs.sql.DbRow
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.convertSnowflake
import me.mrkirby153.KirBot.utils.uploadToArchive
import java.text.SimpleDateFormat

@Command(name = "archive", clearance = CLEARANCE_MOD)
class CommandArchive : BaseCommand(false) {
    override fun execute(context: Context, cmdContext: CommandContext) {
    }

    @Command(name = "user", clearance = CLEARANCE_MOD,
            arguments = ["<user:snowflake>", "[amount:int]"])
    fun archiveUser(context: Context, cmdContext: CommandContext) {
        val amount = cmdContext.get<Int>("amount") ?: 50
        val user = cmdContext.get<String>("user")!!
        val messages = DB.getResults(
                "SELECT * FROM server_messages WHERE author = ? AND server_id = ? ORDER BY id DESC LIMIT ?", user,
                context.guild.id, amount).map { decode(it) }.reversed()
        val archiveUrl = createArchive(messages)
        context.channel.sendMessage("Archived ${messages.count()} messages at $archiveUrl").queue()
    }

    @Command(name = "channel", clearance = CLEARANCE_MOD,
            arguments = ["<channel:snowflake>", "[amount:int]"])
    fun archiveChannel(context: Context, cmdContext: CommandContext) {
        val amount = cmdContext.get<Int>("amount") ?: 50
        val chan = cmdContext.get<String>("channel")!!
        if(context.guild.getTextChannelById(chan) == null)
            throw CommandException("That channel doesn't exist")
        val messages = DB.getResults(
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
            val results = DB.getResults(
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
        msg.id = row.get("id")
        msg.serverId = row.get("server_id")
        msg.author = row.get("author")
        msg.channel = row.get("channel")
        msg.message = LogManager.decrypt(row.get("message"))
        msg._created_at = row.get("created_at")
        msg._updated_at = row.get("updated_at")
        msg.editCount = row.get("edit_count")
        msg.deleted = row.get("deleted")
        msg.attachments = row.get("attachments")
        return msg
    }
}