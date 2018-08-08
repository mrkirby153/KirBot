package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Logger
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.canAssign
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent

@Command(name = "role,r,roles", clearance = CLEARANCE_MOD)
class RoleCommands : BaseCommand(false) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        var msg = "```"
        context.guild.roles.forEach {
            if (msg.length >= 1900) {
                context.channel.sendMessage("$msg```").queue()
                msg = "```"
            }
            msg += "\n${it.id} - ${it.name}"
        }
        context.channel.sendMessage("$msg```").queue()
    }

    /**
     * Checks if the given member can assign the given role
     *
     * @param member The member
     * @param role The role to assign
     *
     * @throws CommandException If an error occurs in the comparison
     */
    private fun checkAssignment(member: Member, role: Role, mode: String = "assign") {
        if (member.isOwner)
            return // The owner can assign all roles regardless of permissions
        val highestPos = member.roles.maxBy { it.position }?.position ?: 0
        val rolePos = role.position
        if (rolePos >= highestPos) {
            throw CommandException("You cannot $mode roles above your own")
        }
    }

    /**
     * Checks if the given member can interact with the other member
     *
     * @param member The member to check
     * @param otherMember The member to check against
     *
     * @throws CommandException If the user cannot manipulate the user
     */
    private fun checkManipulate(member: Member, otherMember: Member) {
        if (member.user.id == otherMember.user.id)
            throw CommandException("You cannot execute that on yourself")
        if (member.isOwner)
            return  // The owner can manipulate everyone regardless of permissions
        val highestPos = member.roles.maxBy { it.position }?.position ?: 0
        val otherPos = otherMember.roles.maxBy { it.position }?.position ?: 0
        if (otherPos >= highestPos)
            throw CommandException("You cannot execute this on that user")
    }

    @Command(name = "add", clearance = CLEARANCE_MOD,
            arguments = ["<user:snowflake>", "<role:string>", "[reason:string...]"],
            permissions = [Permission.MANAGE_ROLES])
    fun addRole(context: Context, cmdContext: CommandContext) {
        val roleString = cmdContext.get<String>("role")!!
        val reason = cmdContext.get<String>("reason") ?: "No reason specified"

        val role: Role?
        try {
            role = context.guild.kirbotGuild.matchRole(roleString) ?: throw CommandException(
                    "No roles found for that query")
        } catch (e: KirBotGuild.TooManyRolesException) {
            throw CommandException(
                    "Multiple matches for that query. Try a more specific query or the role id")
        }


        val member = context.guild.getMemberById(cmdContext.get<String>("user"))
                ?: throw CommandException("That user is not in the guild")

        val m = context.author.getMember(context.guild)
        checkManipulate(m, member)
        checkAssignment(m, role)

        if (!context.guild.selfMember.canAssign(role))
            throw CommandException("I cannot assign that role")

        ModuleManager[Logger::class.java].debouncer.create(GuildMemberRoleAddEvent::class.java,
                Pair("user", member.user.id), Pair("role", role.id))
        context.guild.controller.addSingleRoleToMember(member, role).queue()
        context.kirbotGuild.logManager.genericLog(LogEvent.ROLE_ADD, ":key:",
                "Assigned **${role.name}** to ${member.user.logName}: `$reason`")
        context.send().success("Added role **${role.name}** to ${member.user.nameAndDiscrim}",
                true).queue()
    }

    @Command(name = "remove,rem", clearance = CLEARANCE_MOD,
            arguments = ["<user:snowflake>", "<role:string>", "[reason:string...]"],
            permissions = [Permission.MANAGE_ROLES])
    fun removeRole(context: Context, cmdContext: CommandContext) {
        val roleString = cmdContext.get<String>("role")!!
        val reason = cmdContext.get<String>("reason") ?: "No reason specified"

        val role: Role?
        try {
            role = context.guild.kirbotGuild.matchRole(roleString) ?: throw CommandException(
                    "No roles found for that query")
        } catch (e: KirBotGuild.TooManyRolesException) {
            throw CommandException(
                    "Multiple matches for that query. Try a more specific query or the role id")
        }

        val member = context.guild.getMemberById(cmdContext.get<String>("user"))
                ?: throw CommandException(
                        "that user is not in the guild")

        val m = context.author.getMember(context.guild)
        checkManipulate(m, member)
        checkAssignment(m, role, "remove")

        ModuleManager[Logger::class].debouncer.create(GuildMemberRoleRemoveEvent::class.java,
                Pair("user", member.user.id), Pair("role", role.id))
        context.kirbotGuild.logManager.genericLog(LogEvent.ROLE_ADD, ":key:",
                "Removed **${role.name}** from ${member.user.logName}: `$reason`")
        context.guild.controller.removeSingleRoleFromMember(member, role).queue()
        context.send().success("Removed role **${role.name}** from ${member.user.nameAndDiscrim}",
                true).queue()
    }
}