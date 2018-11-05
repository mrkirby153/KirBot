package me.mrkirby153.KirBot.command.executors.msc

import com.sun.org.glassfish.gmbal.Description
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.infraction.InfractionType
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.FuzzyMatchException
import me.mrkirby153.KirBot.utils.canAssign
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.Permission
import java.sql.Timestamp
import java.time.Instant

@Command(name = "temprole",
        arguments = ["<user:snowflake>", "<role:string>", "<duration:string>", "[reason:string...]"],
        clearance = CLEARANCE_ADMIN, permissions = [Permission.MANAGE_ROLES])
@LogInModlogs
@Description("Temporarily assign a role to a user")
class TempRoleCommands : BaseCommand(false) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val userId = cmdContext.get<String>("user")!!
        val durationRaw = cmdContext.get<String>("duration")!!
        val reason = cmdContext.get<String>("reason")
        val roleQuery = cmdContext.get<String>("role")!!

        val duration = try {
            Time.parse(durationRaw)
        } catch (e: IllegalArgumentException) {
            throw CommandException(
                    e.message ?: "An unknown error occurred when parsing the duration")
        }

        val timestamp = Instant.now().plusMillis(duration)

        val target = context.guild.getMemberById(userId) ?: throw CommandException("User not found")
        val role = try {
            context.kirbotGuild.matchRole(roleQuery) ?: throw CommandException(
                    "No roles found for that query")
        } catch (e: FuzzyMatchException.TooManyMatchesException) {
            throw CommandException(
                    "Too many matches for that query. Try a more specific query or the role id")
        } catch (e: FuzzyMatchException.NoMatchesException) {
            throw CommandException("No roles found for that query")
        }
        RoleCommands.checkManipulate(context.member, target)
        RoleCommands.checkAssignment(context.member, role)

        if (!context.guild.selfMember.canAssign(role))
            throw CommandException("I cannot assign that role")

        if(role in target.roles)
            throw CommandException("${target.user.nameAndDiscrim} is already in that role")

        context.guild.controller.addSingleRoleToMember(target, role).queue {
            Infractions.createInfraction(userId, context.guild, context.author.id, "${role.name} - $reason",
                    InfractionType.TEMPROLE, Timestamp.from(timestamp), role.id)
            context.send().success(
                    "${target.user.logName} is now in ${role.name} for ${Time.format(1,
                            duration)}").queue()
        }
    }

}