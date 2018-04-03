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
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.scheduler.Schedulable
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
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

        if (timeParsed <= 0) {
            throw CommandException("Please provide a time greater than 0")
        }

        if (context.channel !is TextChannel)
            throw CommandException("This command won't work in PMs")

        context.send().success(
                "Muted ${user.nameAndDiscrim} for ${Time.format(1, timeParsed)}" + buildString {
                    if (reason != null) {
                        append(" (`$reason`)")
                    }
                }, true).queue()
        Infractions.tempMute(user.id, context.guild, context.author.id, timeParsed,
                TimeUnit.MILLISECONDS, reason)
    }

    class UnmuteScheduler(val infId: String, val userId: String, val guild: String) : Schedulable {

        override fun run() {
            val infraction = Model.first(Infraction::class.java, infId)
            infraction?.revoke()

            val user = Bot.shardManager.getUser(userId) ?: return
            val guild = Bot.shardManager.getGuild(guild) ?: return
            val member = guild.getMember(user) ?: return

            Infractions.removeMutedRole(user, guild)
            guild.kirbotGuild.logManager.genericLog(":open_mouth:",
                    "Timed mute (`$infId`) ${user.nameAndDiscrim} expired.")
        }

    }
}