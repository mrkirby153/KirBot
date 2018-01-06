package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.RequiresClearance
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.awt.Color

@Command("unmute,unquiet")
@RequiresClearance(Clearance.BOT_MANAGER)
class CommandUnmute : BaseCommand(false, CommandCategory.MODERATION, Arguments.user("user")) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException(
                "Please specify a user to unmute!")
        val member = user.getMember(context.guild) ?: throw CommandException(
                "The user is not a member of this guild!")

        if (context.channel !is TextChannel) {
            throw CommandException("This command doesn't work in PMs")
        }
        if (!context.channel.checkPermissions(Permission.MANAGE_CHANNEL))
            throw CommandException("Missing the required permission: `Manage Channel`")
        val override = (context.channel as TextChannel).getPermissionOverride(member)
        if (override == null || !override.denied.contains(Permission.MESSAGE_WRITE)) {
            context.send().embed("Warning") {
                color = Color.YELLOW
                description { +"That user isn't muted!" }
            }.rest().queue()
            return
        }
        if (override.denied.size > 1) {
            if (override.denied.contains(Permission.MESSAGE_WRITE))
                override.manager.clear(Permission.MESSAGE_WRITE).queue()
        } else {
            override.delete().queue()
        }
        context.success()
        context.data.logManager.genericLog("User Unmuted",
                "${context.author.name} has unmuted ${member.user.name} in ${context.textChannel.asMention}",
                Color.MAGENTA, context.author)
    }
}