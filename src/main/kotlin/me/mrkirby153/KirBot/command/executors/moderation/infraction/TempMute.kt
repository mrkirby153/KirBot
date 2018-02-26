package me.mrkirby153.KirBot.command.executors.moderation.infraction

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.infraction.Infraction
import me.mrkirby153.KirBot.infraction.InfractionType
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Scheduler
import me.mrkirby153.KirBot.scheduler.Schedulable
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.util.concurrent.TimeUnit

@Command(name = "tempmute", arguments = ["<user:user>", "<time:string>", "[reason:string...]"])
@LogInModlogs
class TempMute : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user")!!
        val member = user.getMember(context.guild) ?: throw CommandException(
                "This user isn't a part of the guild!")

        val time = cmdContext.get<String>("time")!!

        val reason = cmdContext.get<String>("reason")

        val timeParsed = Time.parse(time)

        if (timeParsed < 0) {
            throw CommandException("Please provide a time greater than 0")
        }

        if (context.channel !is TextChannel)
            throw CommandException("This command won't work in PMs")

        val channel = context.channel as TextChannel
        val override = channel.getPermissionOverride(member) ?: channel.createPermissionOverride(
                member).complete()
        override.manager.deny(Permission.MESSAGE_WRITE).queue()

        val inf = Infractions.createInfraction(user.id, context.guild, context.author, reason,
                InfractionType.TEMPMUTE)
        context.kirbotGuild.logManager.genericLog(":zipper_mouth:",
                "${user.nameAndDiscrim} (`${user.id}`) Temp muted for ${Time.format(1,
                        timeParsed)} by ${context.author.nameAndDiscrim}")

        context.send().success(
                "Muted ${user.nameAndDiscrim} for ${Time.format(1, timeParsed)}" + buildString {
                    if (reason != null) {
                        append(" (`$reason`)")
                    }
                }, true).queue()

        ModuleManager[Scheduler::class.java].submit(
                UnmuteScheduler(inf.id.toString(), user.id, context.guild.id, context.channel.id),
                timeParsed, TimeUnit.MILLISECONDS)
    }

    class UnmuteScheduler(val infId: String, val userId: String, val guild: String,
                          val channel: String) : Schedulable {

        override fun run() {
            val infraction = Model.first(Infraction::class.java, infId)
            infraction?.revoke()

            val user = Bot.shardManager.getUser(userId) ?: return
            val guild = Bot.shardManager.getGuild(guild) ?: return
            val member = guild.getMember(user) ?: return
            val c = guild.getTextChannelById(channel) ?: return

            val override = c.getPermissionOverride(member) ?: return
            if (override.denied.size > 1) {
                if (override.denied.contains(Permission.MESSAGE_WRITE))
                    override.manager.clear(Permission.MESSAGE_WRITE).queue()
                else
                    return
            } else {
                override.delete().queue()
            }
            guild.kirbotGuild.logManager.genericLog(":open_mouth:",
                    "Timed mute for ${user.nameAndDiscrim} on #${c.name} expired.")
        }

    }
}