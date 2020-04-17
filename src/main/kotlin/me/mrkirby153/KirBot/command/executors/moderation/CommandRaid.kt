package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.AntiRaid
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.b
import me.mrkirby153.KirBot.utils.uploadToArchive
import net.dv8tion.jda.api.Permission
import javax.inject.Inject

class CommandRaid @Inject constructor(private val antiRaid: AntiRaid){

    @Command(name = "info", arguments = ["<id:string>"], clearance = CLEARANCE_MOD,
            category = CommandCategory.MODERATION, parent = "raid")
    @IgnoreWhitelist
    fun raidInfo(context: Context, cmdContext: CommandContext) {
        val raid = antiRaid.getRaid(context.guild,
                cmdContext.getNotNull("id")) ?: throw CommandException("Raid not found")



        val users = buildString {
            raid.members.forEach { member ->
                appendln("${member.id} (${member.name})")
            }
            appendln()
            appendln()
            appendln("Only IDs:")
            appendln(raid.members.joinToString("\n") { it.id })
        }
        val uploadUrl = uploadToArchive(users)
        val msg = buildString {
            appendln(b("==[ RAID ${raid.id} ] =="))
            appendln("${raid.members.size} members were involved in the raid")
            appendln()
            appendln("View the list of users: $uploadUrl")
        }
        context.channel.sendMessage(msg).queue()
    }

    @Command(name = "ban", arguments = ["<id:string>"], parent = "raid", clearance = CLEARANCE_MOD,
            category = CommandCategory.MODERATION, permissions = [Permission.BAN_MEMBERS])
    @LogInModlogs
    @IgnoreWhitelist
    fun raidBan(context: Context, cmdContext: CommandContext) {
        val raid = antiRaid.getRaid(context.guild,
                cmdContext.getNotNull("id")) ?: throw CommandException("Raid not found")
        antiRaid.punishAllRaiders(context.guild, raid.id, "BAN",
                "Member of raid ${raid.id}")
        context.send().success("Banning ${raid.members.size} raiders").queue()
    }

    @Command(name = "kick", arguments = ["<id:string>"], parent = "raid", clearance = CLEARANCE_MOD,
            category = CommandCategory.MODERATION, permissions = [Permission.KICK_MEMBERS])
    @LogInModlogs
    @IgnoreWhitelist
    fun raidKick(context: Context, cmdContext: CommandContext) {
        val raid = antiRaid.getRaid(context.guild,
                cmdContext.getNotNull("id")) ?: throw CommandException("Raid not found")
        antiRaid.punishAllRaiders(context.guild, raid.id, "KICK",
                "Member of raid ${raid.id}")
        context.send().success("Kicking ${raid.members.size} raiders").queue()
    }

    @Command(name = "unmute", arguments = ["<id:string>"], parent = "raid",
            clearance = CLEARANCE_MOD, category = CommandCategory.MODERATION, permissions = [Permission.MANAGE_ROLES])
    @LogInModlogs
    @IgnoreWhitelist
    fun raidUnmute(context: Context, cmdContext: CommandContext) {
        val raid = antiRaid.getRaid(context.guild,
                cmdContext.getNotNull("id")) ?: throw CommandException("Raid not found")
        antiRaid.unmuteAllRaiders(context.guild, raid.id)
        context.send().success("Unmuting ${raid.members.size} raiders").queue()
    }

    @Command(name = "dismiss", parent = "raid", clearance = CLEARANCE_MOD,
            category = CommandCategory.MODERATION)
    @LogInModlogs
    @IgnoreWhitelist
    fun dismiss(context: Context, cmdContext: CommandContext) {
        val antiRaid = antiRaid
        val raid = antiRaid.activeRaids[context.guild.id]
                ?: throw CommandException("There is no active raid")
        antiRaid.dismissActiveRaid(context.guild)
        context.send().success("Dismissed raid and unmuted members").queue()
    }
}