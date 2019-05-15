package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.FuzzyMatchException
import net.dv8tion.jda.core.entities.Role


class CommandSelfroles {

    @Command(name = "selfrole", category = CommandCategory.UTILITY)
    @CommandDescription("Displays a list of self-assignable roles")
    fun execute(context: Context, cmdContext: CommandContext) {
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

    @Command(name = "join", arguments = ["<role:string...>"], parent = "selfrole", category = CommandCategory.UTILITY)
    @CommandDescription("Join a self-assignable role")
    fun join(context: Context, cmdContext: CommandContext) {
        val role = cmdContext.get<String>("role")!!
        val foundRole = findSelfassignRole(context, role)
        if (foundRole.id in context.member.roles.map { it.id })
            throw CommandException("You are already in `${foundRole.name}`")
        if (foundRole.position >= context.guild.selfMember.roles.sortedByDescending { it.position }.first().position)
            throw CommandException(
                    "That role is above my highest role. I can't assign that to you!")
        context.guild.controller.addSingleRoleToMember(context.member, foundRole).queue()
        context.send().success("Joined role `${foundRole.name}`", true).queue()
    }

    @Command(name = "leave", arguments = ["<role:string...>"], parent = "selfrole", category = CommandCategory.UTILITY)
    @CommandDescription("Leave a self-assignable role")
    fun leave(context: Context, cmdContext: CommandContext) {
        val role = cmdContext.get<String>("role")!!
        val foundRole = findSelfassignRole(context, role)
        if (foundRole.id !in context.member.roles.map { it.id })
            throw CommandException("You are not in `${foundRole.name}`")
        if (foundRole.position >= context.guild.selfMember.roles.sortedByDescending { it.position }.first().position)
            throw CommandException(
                    "That role is above my highest role. I can't remove that from you!")
        context.guild.controller.removeSingleRoleFromMember(context.member, foundRole).queue()
        context.send().success("Left role `${foundRole.name}`", true).queue()
    }

    @Command(name = "add", arguments = ["<role:string...>"], clearance = CLEARANCE_ADMIN,
            parent = "selfrole", category = CommandCategory.UTILITY)
    @CommandDescription("Add a role to the list of self-assignable roles")
    @IgnoreWhitelist
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
        } catch (e: FuzzyMatchException.TooManyMatchesException) {
            throw CommandException(
                    "Too many matches for that query. Try a more specific query or the role's id instead")
        } catch (e: FuzzyMatchException.NoMatchesException) {
            throw CommandException("No role was found for that query")
        }
    }

    @Command(name = "remove", arguments = ["<role:string...>"], clearance = CLEARANCE_ADMIN,
            parent = "selfrole", category = CommandCategory.UTILITY)
    @CommandDescription("Removes a role from the list of self-assignable roles")
    @IgnoreWhitelist
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
        } catch (e: FuzzyMatchException.TooManyMatchesException) {
            throw CommandException(
                    "Too many matches for that query. Try a more specific query or the role's id instead")
        } catch (e: FuzzyMatchException.NoMatchesException) {
            throw CommandException("No role was found for that query")
        }
    }

    private fun findSelfassignRole(context: Context, query: String): Role {
        try {
            val foundRole = context.kirbotGuild.matchRole(query)
            if (foundRole == null || foundRole.id !in context.kirbotGuild.getSelfroles())
                throw CommandException("No self-assignable role was found with that query")
            return foundRole
        } catch (e: FuzzyMatchException.TooManyMatchesException) {
            throw CommandException(
                    "Too many roles were found for the query. Try a more specific query or the role id")
        } catch (e: FuzzyMatchException.NoMatchesException) {
            throw CommandException("No role was found for that query")
        }
    }
}