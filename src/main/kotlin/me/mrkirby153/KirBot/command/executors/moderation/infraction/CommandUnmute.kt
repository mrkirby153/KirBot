package me.mrkirby153.KirBot.command.executors.moderation.infraction

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.infraction.Infraction
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.sql.Timestamp

@Command(name = "unmute,unquiet", arguments = ["<user:user>"], clearance = Clearance.BOT_MANAGER)
@LogInModlogs
class CommandUnmute : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException(
                "Please specify a user to unmute!")
        val member = user.getMember(context.guild) ?: throw CommandException(
                "The user is not a member of this guild!")

        if (context.channel !is TextChannel) {
            throw CommandException("This command doesn't work in PMs")
        }
        // Remove the infraction
        Model.get(Infraction::class.java, Pair("user_id", user.id), Pair("guild", context.guild.id),
                Pair("type", "mute"), Pair("active", true)).forEach {
            it.active = false
            it.revokedAt = Timestamp(System.currentTimeMillis())
            it.save()
        }
        Infractions.removeMutedRole(user, context.guild)
        context.send().success("Unmuted **${user.name}#${user.discriminator}**", true).queue()
        context.kirbotGuild.logManager.genericLog(":open_mouth:",
                "${member.user.nameAndDiscrim} (`${member.user.id}`) Unmuted by ${context.author.nameAndDiscrim}")
    }
}