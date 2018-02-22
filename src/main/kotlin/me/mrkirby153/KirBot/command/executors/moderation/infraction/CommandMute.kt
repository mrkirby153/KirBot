package me.mrkirby153.KirBot.command.executors.moderation.infraction

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.infraction.InfractionType
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User

@Command(name = "mute,shutup,quiet", arguments = ["<user:user>", "<reason:string,rest>"],
        clearance = Clearance.BOT_MANAGER)
class CommandMute : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: throw CommandException(
                "Please specify a user to mute")
        val reason = cmdContext.get<String>("reason") ?: throw CommandException(
                "Please specify a reason")
        val member = user.getMember(context.guild) ?: throw CommandException(
                "This user isn't a part of the guild!")
        if (context.channel !is TextChannel)
            throw CommandException("This command won't work in PMs")

        if (!context.channel.checkPermissions(Permission.MANAGE_CHANNEL))
            throw CommandException("Missing the required permission: `Manage Channel`")
        val channel = context.channel as TextChannel
        val override = channel.getPermissionOverride(member) ?: channel.createPermissionOverride(
                member).complete()
        override.manager.deny(Permission.MESSAGE_WRITE).queue()
        context.send().success("Muted **${user.name}#${user.discriminator}** (`$reason`)",
                true).queue()
        Infractions.createInfraction(user.id, context.guild, context.author, reason,
                InfractionType.MUTE)
        context.kirbotGuild.logManager.genericLog(":zipper_mouth:",
                "${member.user.nameAndDiscrim} (`${member.user.id}`) Muted by ${context.author.nameAndDiscrim} (`$reason`)")
    }
}
