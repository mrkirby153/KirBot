package me.mrkirby153.KirBot.command.executors.`fun`

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.DB
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.guild.GuildMember
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.CustomEmoji
import me.mrkirby153.KirBot.utils.HttpUtils
import me.mrkirby153.KirBot.utils.STATUS_AWAY
import me.mrkirby153.KirBot.utils.STATUS_DND
import me.mrkirby153.KirBot.utils.STATUS_OFFLINE
import me.mrkirby153.KirBot.utils.STATUS_ONLINE
import me.mrkirby153.KirBot.utils.convertSnowflake
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.getOnlineStats
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import okhttp3.Request
import java.awt.Color
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.Date
import javax.imageio.ImageIO


class CommandInfo {

    private val sdf = SimpleDateFormat("YYYY-MM-dd HH:mm:ss")

    @Command(name = "info", arguments = ["[user:user]"], category = CommandCategory.MODERATION,
            clearance = CLEARANCE_MOD)
    @CommandDescription("Retrieves information about a user")
    fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: context.author
        val onlineStatus = user.getOnlineStats()

        val member = Model.where(GuildMember::class.java, "user_id", user.id).where("server_id",
                context.guild.id).first()

        val sentMessages = DB.getFirstColumn<Long>(
                "SELECT COUNT(*) FROM server_messages WHERE `author` = ?", user.id)
        val editedMessages = DB.getFirstColumn<Long>(
                "SELECT COUNT(*) FROM server_messages WHERE `author` = ? AND edit_count > 0",
                user.id)
        val deletedMessages = DB.getFirstColumn<Long>(
                "SELECT COUNT(*) FROM server_messages WHERE `author` = ? AND deleted = 1", user.id)

        val lastMessageId = DB.getFirstColumn<String>(
                "SELECT id FROM server_messages WHERE author = ? AND server_id = ? ORDER BY id DESC LIMIT 1",
                user.id, context.guild.id)
        val firstMessageId = DB.getFirstColumn<String>(
                "SELECT id from server_messages WHERE author = ? AND server_id = ? ORDER BY id ASC LIMIT 1",
                user.id, context.guild.id)

        val jdaMember = user.getMember(context.guild)
        context.send().embed {
            thumbnail = user.effectiveAvatarUrl
            if (jdaMember != null) {
                color = getMostColour(getUserProfile(user))
            }
            author {
                name = "${user.name}#${user.discriminator}"
                iconUrl = user.effectiveAvatarUrl
            }
            description {
                appendln("**> User Information**")
                appendln("ID: ${user.id}")
                appendln("Created: ${Time.formatLong(
                        System.currentTimeMillis() - (user.timeCreated.toEpochSecond() * 1000),
                        Time.TimeUnit.MINUTES)}")
                appendln("Status: $onlineStatus ${getOnlineEmoji(onlineStatus)}")
                appendln("Profile: ${user.asMention}")
                if (jdaMember?.activities?.isNotEmpty() == true)
                    appendln(getPlayingStatus(jdaMember))
                appendln("")
                appendln("**> Member Information**")
                if (jdaMember != null) {
                    val joinTime = jdaMember.timeJoined.toEpochSecond() * 1000
                    appendln("Joined: ${Time.formatLong(
                            System.currentTimeMillis() - joinTime,
                            Time.TimeUnit.MINUTES).toLowerCase()} ago (${SimpleDateFormat(
                            "MM-dd-yy HH:mm:ss").format(joinTime)})")
                }
                if (member != null) {
                    appendln("")
                    appendln("**> Activity**")
                    val firstMsg = if (firstMessageId != null) convertSnowflake(
                            firstMessageId) else null
                    val lastMsg = if (lastMessageId != null) convertSnowflake(
                            lastMessageId) else null
                    val now = Date(System.currentTimeMillis())
                    if (lastMsg != null) {
                        val lastMsgAgo = if (now.time - lastMsg.time <= 1000) "a moment ago" else "${Time.format(
                                0, now.time - lastMsg.time)} ago"
                        appendln("Last Message: $lastMsgAgo (${sdf.format(lastMsg)})")
                    }
                    if (firstMsg != null) {
                        val firstMsgAgo = if (now.time - firstMsg.time <= 2000) "a moment ago" else "${Time.format(
                                0, now.time - firstMsg.time)} ago"
                        appendln("First Message: $firstMsgAgo (${sdf.format(firstMsg)})")
                    }
                    if (lastMsg != null || firstMsg != null)
                        appendln("")
                }
                appendln("Sent Messages: $sentMessages")
                appendln("Edited Messages: $editedMessages")
                appendln("Deleted Messages: $deletedMessages")
            }
        }.rest().queue()
    }

    private fun getOnlineEmoji(status: OnlineStatus): CustomEmoji {
        return when (status) {
            OnlineStatus.ONLINE -> STATUS_ONLINE
            OnlineStatus.IDLE -> STATUS_AWAY
            OnlineStatus.DO_NOT_DISTURB -> STATUS_DND
            OnlineStatus.INVISIBLE -> STATUS_OFFLINE
            OnlineStatus.OFFLINE -> STATUS_OFFLINE
            OnlineStatus.UNKNOWN -> STATUS_OFFLINE
        }
    }

    private fun getPlayingStatus(member: Member): String {
        val activities = member.activities
        return buildString {
            activities.forEach { activity ->
                when (activity.type) {
                    Activity.ActivityType.DEFAULT -> append("Playing ")
                    Activity.ActivityType.LISTENING -> append("Listening to ")
                    Activity.ActivityType.STREAMING -> append("Streaming ")
                    Activity.ActivityType.WATCHING -> append("Watching ")
                    null -> append("Playing ")
                }
                append(activity.name)
            }

        }
    }

    private fun getUserProfile(user: User): BufferedImage {
        val url = user.effectiveAvatarUrl.replace(".gif", ".png")
        val req = Request.Builder().url(url).build()
        val resp = HttpUtils.CLIENT.newCall(req).execute()
        val img = ImageIO.read(resp.body()!!.byteStream())
        resp.close()
        return img
    }

    /**
     * Gets the  colour with the most pixels
     *
     * @param image The image to process
     * @return a [Color] of the most prominent color
     */
    private fun getMostColour(image: BufferedImage): Color {
        val colours = mutableMapOf<Int, Int>()
        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                val c = image.getRGB(x, y)
                val count = colours.computeIfAbsent(c) { 0 }
                colours[c] = count + 1
            }
        }
        var max = 0
        var maxCount = 0
        colours.forEach { k, v ->
            if (maxCount < v) {
                max = k
                maxCount = v
            }
        }
        return Color(max)
    }
}