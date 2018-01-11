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

@Command("mute,shutup,quiet")
@RequiresClearance(Clearance.BOT_MANAGER)
class CommandMute : BaseCommand(false, CommandCategory.MODERATION, Arguments.user("user")) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException("Please specify a user to mute")

        val member = user.getMember(context.guild) ?: throw CommandException("This user isn't a part of the guild!")
        if (context.channel !is TextChannel)
            throw CommandException("This command won't work in PMs")

        if (!context.channel.checkPermissions(Permission.MANAGE_CHANNEL))
            throw CommandException("Missing the required permission: `Manage Channel`")
        val channel = context.channel as TextChannel
        val override = channel.getPermissionOverride(member) ?: channel.createPermissionOverride(member).complete()
        override.manager.deny(Permission.MESSAGE_WRITE).queue()
        context.success()
        context.kirbotGuild.logManager.genericLog("User Muted", "${context.author.name} has muted ${member.user.name} in ${context.textChannel.asMention}", Color.MAGENTA, context.author)
    }
}
