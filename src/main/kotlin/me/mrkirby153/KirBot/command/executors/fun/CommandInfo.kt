package me.mrkirby153.KirBot.command.executors.`fun`

import co.aikar.idb.DB
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
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
class CommandInfo : BaseCommand(CommandCategory.FUN) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: context.author

        val seenData = Bot.seenStore.get(user)
        val member = Model.first(GuildMember::class.java, Pair("user_id", user.id),
                Pair("server_id", context.guild.id))

        val sentMessages = DB.getFirstColumn<Long>("SELECT COUNT(*) FROM server_messages WHERE `author` = ?", user.id)
        val editedMessages = DB.getFirstColumn<Long>("SELECT COUNT(*) FROM server_messages WHERE `author` = ? AND edit_count > 0", user.id)
        val deletedMessages = DB.getFirstColumn<Long>("SELECT COUNT(*) FROM server_messages WHERE `author` = ? and deleted = 1", user.id)

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
                if (member != null) {
                    appendln("**> Member Information**")
                    if (member.created_at != null) {
                        appendln("Joined: ${Time.formatLong(
                                System.currentTimeMillis() - member.created_at!!.time,
                                Time.TimeUnit.MINUTES).toLowerCase()} ago (${SimpleDateFormat(
                                Time.DATE_FORMAT_NOW).format(member.created_at)})")
                    }
                    appendln("")
                    appendln("**> Activity**")
                    if (seenData != null)
                        appendln("Last Message: ${Time.format(1,
                                System.currentTimeMillis() - seenData.lastMessage)} ago")
                    appendln("Sent Messages: $sentMessages")
                    appendln("Edited Messages: $editedMessages")
                    appendln("Deleted Messages: $deletedMessages")
                }
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