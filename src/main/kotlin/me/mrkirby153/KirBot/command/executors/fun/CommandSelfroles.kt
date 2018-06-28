package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.entities.Role

@Command(name = "selfrole,selfroles")
class CommandSelfroles : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        var msg = "Self-assignable roles are:\n```\n"
        val selfroles = context.kirbotGuild.getSelfroles()
        selfroles.mapNotNull {
            context.guild.getRoleById(it)
        }.forEach {
            msg += "\n${it.id} - ${it.name}"
            if (msg.length > 1900) {
                msg += "```"
                context.channel.sendMessage(msg).queue()
                msg = "```"
            }
        }
        msg += "```"
        context.channel.sendMessage(msg).queue()
    }

    @Command(name = "join", arguments = ["<role:string...>"])
    fun join(context: Context, cmdContext: CommandContext) {
        val role = cmdContext.get<String>("role")!!
        val foundRole = findSelfassignRole(context, role)
        if (foundRole.id in context.member.roles.map { it.id })
            throw CommandException("You are already in `${foundRole.name}`")
        if (foundRole.position >= context.jda.selfUser.getMember(
                        context.guild).roles.sortedByDescending { it.position }.first().position)
            throw CommandException(
                    "That role is above my highest role. I can't assign that to you!")
        context.guild.controller.addSingleRoleToMember(context.member, foundRole).queue()
        context.send().success("Joined role `${foundRole.name}`", true).queue()
    }

    @Command(name = "leave", arguments = ["<role:string...>"])
    fun leave(context: Context, cmdContext: CommandContext) {
        val role = cmdContext.get<String>("role")!!
        val foundRole = findSelfassignRole(context, role)
        if (foundRole.id !in context.member.roles.map { it.id })
            throw CommandException("You are not in `${foundRole.name}`")
        if (foundRole.position >= context.jda.selfUser.getMember(
                        context.guild).roles.sortedByDescending { it.position }.first().position)
            throw CommandException(
                    "That role is above my highest role. I can't remove that from you!")
        context.guild.controller.removeSingleRoleFromMember(context.member, foundRole).queue()
        context.send().success("Left role `${foundRole.name}`", true).queue()
    }

    @Command(name = "add", arguments = ["<role:string...>"], clearance = CLEARANCE_ADMIN)
    fun add(context: Context, cmdContext: CommandContext) {
        try {
            val role = cmdContext.get<String>("role")!!
            val foundRole = context.kirbotGuild.matchRole(role) ?: throw CommandException(
                    "No role was found for that query")
            if (foundRole.id in context.kirbotGuild.getSelfroles())
                throw CommandException("`${foundRole.name}` already is a selfrole!")
            context.kirbotGuild.createSelfrole(foundRole.id)
            context.send().success(
                    "Added `${foundRole.name}` to the list of self assignable roles!").queue()
        } catch (e: KirBotGuild.TooManyRolesException) {
            throw CommandException(
                    "Too many matches for that query. Try a more specific query or the role's id instead")
        }
    }

    @Command(name = "remove", arguments = ["<role:string...>"], clearance = CLEARANCE_ADMIN)
    fun remove(context: Context, cmdContext: CommandContext) {
        val role = cmdContext.get<String>("role")!!
        try {
            val foundRole = context.kirbotGuild.matchRole(role) ?: throw CommandException(
                    "No role was found for that query")
            if (foundRole.id !in context.kirbotGuild.getSelfroles())
                throw CommandException("`${foundRole.name}` isn't a selfrole!")
            context.kirbotGuild.removeSelfrole(foundRole.id)
            context.send().success(
                    "Removed `${foundRole.name}` from the list of self assignable roles!").queue()
        } catch(e: KirBotGuild.TooManyRolesException){
            throw CommandException(
                    "Too many matches for that query. Try a more specific query or the role's id instead")
        }
    }

    private fun findSelfassignRole(context: Context, query: String): Role {
        try {
            val foundRole = context.kirbotGuild.matchRole(query)
            if (foundRole == null || foundRole.id !in context.kirbotGuild.getSelfroles())
                throw CommandException("No self-assignable role was found with that query")
            return foundRole
        } catch (e: KirBotGuild.TooManyRolesException) {
            throw CommandException(
                    "Too many roles were found for the query. Try a more specific query or the role id")
        }
    }
}