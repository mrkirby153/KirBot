package me.mrkirby153.KirBot.command.executors.`fun`

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.DB
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.guild.GuildMember
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.CustomEmoji
import me.mrkirby153.KirBot.utils.STATUS_AWAY
import me.mrkirby153.KirBot.utils.STATUS_DND
import me.mrkirby153.KirBot.utils.STATUS_OFFLINE
import me.mrkirby153.KirBot.utils.STATUS_ONLINE
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.User
import java.text.SimpleDateFormat

@Command(name = "info", arguments = ["[user:user]"])
@CommandDescription("Retrieves information about a user")
class CommandInfo : BaseCommand(CommandCategory.FUN) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: context.author

        val seenData = Bot.seenStore.get(user)
        val member = Model.where(GuildMember::class.java, "user_id", user.id).where("server_id",
                context.guild.id).first()

        val sentMessages = DB.getFirstColumn<Long>(
                "SELECT COUNT(*) FROM server_messages WHERE `author` = ?", user.id)
        val editedMessages = DB.getFirstColumn<Long>(
                "SELECT COUNT(*) FROM server_messages WHERE `author` = ? AND edit_count > 0",
                user.id)
        val deletedMessages = DB.getFirstColumn<Long>(
                "SELECT COUNT(*) FROM server_messages WHERE `author` = ? AND deleted = 1", user.id)

        context.send().embed {
            thumbnail = user.effectiveAvatarUrl
            if (user.getMember(context.guild) != null)
                color = user.getMember(context.guild).color
            author {
                name = "${user.name}#${user.discriminator}"
                iconUrl = user.effectiveAvatarUrl
            }
            description {
                appendln("**> User Information**")
                appendln("ID: ${user.id}")
                if (seenData != null)
                    appendln("Status: ${seenData.status.name} ${getOnlineEmoji(seenData.status)}")
                else
                    appendln("Status: Unknown")
                appendln("Profile: ${user.asMention}")
                if (user.getMember(context.guild).game != null)
                    appendln(getPlayingStatus(user.getMember(context.guild)))
                appendln("")
                appendln("**> Member Information**")
                if (user.getMember(context.guild) != null) {
                    val joinTime = user.getMember(context.guild).joinDate.toEpochSecond() * 1000
                    appendln("Joined: ${Time.formatLong(
                            System.currentTimeMillis() - joinTime,
                            Time.TimeUnit.MINUTES).toLowerCase()} ago (${SimpleDateFormat(
                            Time.DATE_FORMAT_NOW).format(joinTime)})")
                }
                if (member != null) {
                    appendln("")
                    appendln("**> Activity**")
                    if (seenData != null)
                        appendln("Last Message: ${Time.format(1,
                                System.currentTimeMillis() - seenData.lastMessage)} ago")
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
        val game = member.game
        return buildString {
            when (game.type) {
                Game.GameType.DEFAULT -> append("Playing ")
                Game.GameType.LISTENING -> append("Listening to ")
                Game.GameType.STREAMING -> append("Streaming ")
                Game.GameType.WATCHING -> append("Watching ")
                null -> append("Playing ")
            }
            append(game.name)
        }
    }
}