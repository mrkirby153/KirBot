package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.Arguments
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

@Command("info")
class CommandInfo : BaseCommand(CommandCategory.FUN, Arguments.user("user", false)) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: context.author

        val data = Bot.seenStore.get(user) ?: throw CommandException("No data recorded for user")
        val member = Model.first(GuildMember::class.java, Pair("user_id", user.id),
                Pair("server_id", context.guild.id)) ?: throw CommandException(
                "The user isn't in this guild")

        val joinDate = member.created_at
        context.send().embed {
            thumbnail = user.effectiveAvatarUrl
            color = user.getMember(context.guild).color
            author {
                name = "${user.name}#${user.discriminator}"
                iconUrl = user.effectiveAvatarUrl
            }
            description {
                appendln("**> User Information**")
                appendln("ID: ${user.id}")
                appendln("Status: ${data.status.name} ${getOnlineEmoji(data.status)}")
                appendln("Profile: ${user.asMention}")
                if (user.getMember(context.guild).game != null)
                    appendln("Status: " + getPlayingStatus(user.getMember(context.guild)))
                appendln("")
                appendln("**> Member Information**")
                if (joinDate != null)
                    appendln("Joined: ${Time.format(0,
                            System.currentTimeMillis() - joinDate.time)} ago (${SimpleDateFormat(
                            Time.DATE_FORMAT_NOW).format(joinDate)})")
                appendln("")
                appendln("**> Activity**")
                appendln("Last Message: ${Time.format(1,
                        System.currentTimeMillis() - data.lastMessage, Time.TimeUnit.FIT,
                        Time.TimeUnit.SECONDS)} ago (${SimpleDateFormat(
                        Time.DATE_FORMAT_NOW).format(data.lastMessage)})")
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