package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.AntiRaid
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.b
import net.dv8tion.jda.core.MessageBuilder

@Command(name = "raid", clearance = CLEARANCE_MOD)
class CommandRaid : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
    }

    @Command(name = "info", arguments = ["<id:string>"])
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

    @Command(name = "ban", arguments = ["<id:string>"])
    fun raidBan(context: Context, cmdContext: CommandContext) {
        val raid = ModuleManager[AntiRaid::class.java].getRaid(context.guild,
                cmdContext.getNotNull("id")) ?: throw CommandException("Raid not found")
        ModuleManager[AntiRaid::class.java].punishAllRaiders(context.guild, raid.id, "BAN",
                "Member of raid ${raid.id}")
        context.send().success("Banning ${raid.members.size} raiders").queue()
    }

    @Command(name = "kick", arguments = ["<id:string>"])
    fun raidKick(context: Context, cmdContext: CommandContext) {
        val raid = ModuleManager[AntiRaid::class.java].getRaid(context.guild,
                cmdContext.getNotNull("id")) ?: throw CommandException("Raid not found")
        ModuleManager[AntiRaid::class.java].punishAllRaiders(context.guild, raid.id, "KICK",
                "Member of raid ${raid.id}")
        context.send().success("Kicking ${raid.members.size} raiders").queue()
    }

    @Command(name = "unmute", arguments = ["<id:string>"])
    fun raidUnmute(context: Context, cmdContext: CommandContext) {
        val raid = ModuleManager[AntiRaid::class.java].getRaid(context.guild,
                cmdContext.getNotNull("id")) ?: throw CommandException("Raid not found")
        ModuleManager[AntiRaid::class.java].unmuteAllRaiders(context.guild, raid.id)
        context.send().success("Unmuting ${raid.members.size} raiders").queue()
    }

    @Command(name = "dismiss")
    fun dismiss(context: Context, cmdContext: CommandContext) {
        val antiRaid = ModuleManager[AntiRaid::class.java]
        val raid = antiRaid.activeRaids[context.guild.id]
                ?: throw CommandException("There is no active raid")
        antiRaid.dismissActiveRaid(context.guild)
        context.send().success("Dismissed raid and unmuted members").queue()
    }
}