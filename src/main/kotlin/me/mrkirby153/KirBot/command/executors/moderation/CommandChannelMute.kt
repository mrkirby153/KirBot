package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Scheduler
import me.mrkirby153.KirBot.scheduler.Schedulable
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import java.util.concurrent.TimeUnit

@Command(name = "chanmute,cmute", arguments = ["<user:snowflake>", "<time:string>"], clearance = CLEARANCE_MOD)
@LogInModlogs
class CommandChannelMute : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val userId = cmdContext.get<String>("user")!!
        val timeRaw = cmdContext.get<String>("time")!!

        val timeParsed = Time.parse(timeRaw)

        if (timeParsed <= 0)
            throw CommandException("Specify a time greater than zero!")

        val user = Bot.shardManager.getUser(userId) ?: throw CommandException(
                "That user was not found")
        val member = context.guild.getMember(user) ?: throw CommandException(
                "That user isn't in this guild!")

        val channel = context.channel as TextChannel

        val override = channel.getPermissionOverride(member)

        if (override == null) {
            channel.createPermissionOverride(member).setDeny(Permission.MESSAGE_WRITE).queue()
        } else {
            override.manager.deny(Permission.MESSAGE_WRITE).queue()
        }
        val id = ModuleManager[Scheduler::class.java].submit(
                UnmuteScheduler(context.guild.id, channel.id, user.id), timeParsed,
                TimeUnit.MILLISECONDS)
        Bot.LOG.debug("Submitted unmuter as  $id")
        context.kirbotGuild.logManager.genericLog(LogEvent.USER_MUTE, ":zipper_mouth:", "${user.nameAndDiscrim} (`${user.id}`) is now muted for ${Time.formatLong(timeParsed)}")
        context.send().success(
                "${user.nameAndDiscrim} (`${user.id}`) is now muted in this channel for ${Time.formatLong(
                        timeParsed)}", hand = true).queue()
    }
}

@Command(name = "chanunmute,cunmute", arguments = ["<user:snowflake>"], clearance = CLEARANCE_MOD)
@LogInModlogs
class CommandChanUnmute : BaseCommand() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val userId = cmdContext.get<String>("user")!!
        val user = Bot.shardManager.getUser(userId) ?: throw CommandException(
                "That user was not found")
        val member = context.guild.getMember(user) ?: throw CommandException(
                "That user isn't in this guild!")

        val override = (context.channel as TextChannel).getPermissionOverride(
                member)
        if (override == null || Permission.MESSAGE_WRITE !in override.denied)
            throw CommandException("That user isn't muted!")

        val manager = override.managerUpdatable
        manager.clear(Permission.MESSAGE_WRITE)
        if (manager.denyBits == null || manager.denyBits == 0L)
            override.delete().queue()
        else
            manager.update().queue()
        context.kirbotGuild.logManager.genericLog(LogEvent.USER_UNMUTE, ":open_mouth:",
                "${user.nameAndDiscrim} was unmuted in *${context.channel.name}* by ${context.author.nameAndDiscrim}")
        context.send().success("Unmuted ${context.author.nameAndDiscrim}").queue()
    }

}

class UnmuteScheduler(val guildId: String, val channelId: String, val userId: String) :
        Schedulable {
    override fun run() {
        val guild = Bot.shardManager.getGuild(guildId) ?: return
        val channel = guild.getTextChannelById(channelId) ?: return
        val user = Bot.shardManager.getUser(userId) ?: return
        val member = guild.getMember(user) ?: return
        val override = channel.getPermissionOverride(member) ?: return

        guild.kirbotGuild.logManager.genericLog(LogEvent.USER_UNMUTE, ":open_mouth:",
                "Timed mute in *${channel.name}* expired for ${user.nameAndDiscrim}")

        val manager = override.managerUpdatable
        manager.clear(Permission.MESSAGE_WRITE)
        if (manager.denyBits == null || manager.denyBits == 0L) {
            override.delete().queue()
        } else {
            manager.update().queue()
        }
    }

}