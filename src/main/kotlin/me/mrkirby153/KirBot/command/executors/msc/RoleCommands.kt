package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.canAssign
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Role

@Command(name = "role,r,roles", clearance = CLEARANCE_MOD)
class RoleCommands : BaseCommand(false) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        var msg = "```"
        context.guild.roles.forEach {
            if(msg.length >= 1900){
                context.channel.sendMessage("$msg```").queue()
                msg = "```"
            }
            msg += "\n${it.id} - ${it.name}"
        }
        context.channel.sendMessage("$msg```").queue()
    }

    @Command(name = "add", clearance = CLEARANCE_MOD,
            arguments = ["<user:snowflake>", "<role:string...>"], permissions = [Permission.MANAGE_ROLES])
    fun addRole(context: Context, cmdContext: CommandContext) {
        val roleString = cmdContext.get<String>("role")!!

        val role : Role?
        try {
            role = context.guild.kirbotGuild.matchRole(roleString) ?: throw CommandException("No roles found for that query")
        } catch (e: KirBotGuild.TooManyRolesException) {
            throw CommandException("Multiple matches for that query. Try a more specific query or the role id")
        }


        val member = context.guild.getMemberById(cmdContext.get<String>("user")) ?: throw CommandException("That user is not in the guild")

        val m = context.member
        val highestRole = m.roles.map { it.position }.max() ?: 0

        val targetHighest = member.roles.map { it.position }.max() ?: 0


        if (member.user.id == context.author.id)
            throw CommandException("you cannot execute this on yourself!")

        if (targetHighest >= highestRole)
            throw CommandException("you cannot execute that on this user")

        if (!context.member.canAssign(role))
            throw CommandException("you cannot add roles above yourself")

        if(!context.guild.selfMember.canAssign(role))
            throw CommandException("I cannot assign that role")

        context.guild.controller.addSingleRoleToMember(member, role).queue()
        context.send().success("Added role **${role.name}** to ${member.user.nameAndDiscrim}",
                true).queue()
    }

    @Command(name = "remove,rem", clearance = CLEARANCE_MOD,
            arguments = ["<user:snowflake>", "<role:string...>"], permissions = [Permission.MANAGE_ROLES])
    fun removeRole(context: Context, cmdContext: CommandContext) {
        val roleString = cmdContext.get<String>("role")!!

        val role : Role?
        try {
            role = context.guild.kirbotGuild.matchRole(roleString) ?: throw CommandException("No roles found for that query")
        } catch (e: KirBotGuild.TooManyRolesException) {
            throw CommandException("Multiple matches for that query. Try a more specific query or the role id")
        }

        val member = context.guild.getMemberById(cmdContext.get<String>("user"))
                ?: throw CommandException(
                        "that user is not in the guild")

        val highestRole = context.member.roles.map { it.position }.max()!!

        val targetHighest = member.roles.map { it.position }.max()!!

        if (member.user.id == context.author.id)
            throw CommandException("you cannot execute this on yourself!")

        if (targetHighest >= highestRole)
            throw CommandException("you cannot execute that on this user")

        if (!context.member.canAssign(role))
            throw CommandException("you cannot add roles above yourself")

        if(!context.guild.selfMember.canAssign(role))
            throw CommandException("I cannot assign that role")

        context.guild.controller.removeSingleRoleFromMember(member, role).queue()
        context.send().success("Removed role **${role.name}** to ${member.user.nameAndDiscrim}",
                true).queue()
    }
}