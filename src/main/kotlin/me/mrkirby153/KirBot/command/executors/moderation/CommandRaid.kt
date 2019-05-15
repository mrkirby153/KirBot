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
import net.dv8tion.jda.core.MessageBuilder

class CommandRaid {

    @Command(name = "info", arguments = ["<id:string>"], clearance = CLEARANCE_MOD, category = CommandCategory.MODERATION, parent = "raid")
    @IgnoreWhitelist
    fun raidInfo(context: Context, cmdContext: CommandContext) {
        val raid = ModuleManager[AntiRaid::class.java].getRaid(context.guild,
                cmdContext.getNotNull("id")) ?: throw CommandException("Raid not found")

        val msg = buildString {
            appendln(b("==[ RAID ${raid.id} ] =="))
            appendln("${raid.members.size} members were involved in the raid")
        }

        val users = buildString {
            raid.members.forEach { member ->
                appendln("${member.id} (${member.name})")
            }
            appendln()
            appendln()
            appendln("Only IDs:")
            appendln(raid.members.joinToString("\n") { it.id })
        }
        context.channel.sendFile(users.toByteArray(),
                "raid_members_${cmdContext.getNotNull<String>("id")}.txt",
                MessageBuilder(msg).build()).queue()
    }

    @Command(name = "ban", arguments = ["<id:string>"], parent = "raid", clearance = CLEARANCE_MOD, category = CommandCategory.MODERATION)
    @LogInModlogs
    @IgnoreWhitelist
    fun raidBan(context: Context, cmdContext: CommandContext) {
        val raid = ModuleManager[AntiRaid::class.java].getRaid(context.guild,
                cmdContext.getNotNull("id")) ?: throw CommandException("Raid not found")
        ModuleManager[AntiRaid::class.java].punishAllRaiders(context.guild, raid.id, "BAN",
                "Member of raid ${raid.id}")
        context.send().success("Banning ${raid.members.size} raiders").queue()
    }

    @Command(name = "kick", arguments = ["<id:string>"], parent = "raid", clearance = CLEARANCE_MOD, category = CommandCategory.MODERATION)
    @LogInModlogs
    @IgnoreWhitelist
    fun raidKick(context: Context, cmdContext: CommandContext) {
        val raid = ModuleManager[AntiRaid::class.java].getRaid(context.guild,
                cmdContext.getNotNull("id")) ?: throw CommandException("Raid not found")
        ModuleManager[AntiRaid::class.java].punishAllRaiders(context.guild, raid.id, "KICK",
                "Member of raid ${raid.id}")
        context.send().success("Kicking ${raid.members.size} raiders").queue()
    }

    @Command(name = "unmute", arguments = ["<id:string>"], parent = "raid", clearance = CLEARANCE_MOD, category = CommandCategory.MODERATION)
    @LogInModlogs
    @IgnoreWhitelist
    fun raidUnmute(context: Context, cmdContext: CommandContext) {
        val raid = ModuleManager[AntiRaid::class.java].getRaid(context.guild,
                cmdContext.getNotNull("id")) ?: throw CommandException("Raid not found")
        ModuleManager[AntiRaid::class.java].unmuteAllRaiders(context.guild, raid.id)
        context.send().success("Unmuting ${raid.members.size} raiders").queue()
    }

    @Command(name = "dismiss", parent = "raid", clearance = CLEARANCE_MOD, category = CommandCategory.MODERATION)
    @LogInModlogs
    @IgnoreWhitelist
    fun dismiss(context: Context, cmdContext: CommandContext) {
        val antiRaid = ModuleManager[AntiRaid::class.java]
        val raid = antiRaid.activeRaids[context.guild.id]
                ?: throw CommandException("There is no active raid")
        antiRaid.dismissActiveRaid(context.guild)
        context.send().success("Dismissed raid and unmuted members").queue()
    }
}