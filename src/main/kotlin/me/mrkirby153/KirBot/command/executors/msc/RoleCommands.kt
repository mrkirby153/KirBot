package me.mrkirby153.KirBot.command.executors.msc

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Role

@Command(name = "role,r", clearance = CLEARANCE_MOD)
class RoleCommands : BaseCommand(false) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        context.channel.sendMessage(buildString {
            append("```\n")
            context.guild.roles.forEach {
                append(it.name)
                append(" (${it.id})\n")
            }
            append("```")
        }).queue()
    }

    @Command(name = "add", clearance = CLEARANCE_MOD,
            arguments = ["<user:snowflake>", "<role:string...>"])
    fun addRole(context: Context, cmdContext: CommandContext) {
        val roleString = cmdContext.get<String>("role")!!

        val roles = findRoles(roleString, context.guild)

        val member = context.guild.getMemberById(cmdContext.get<String>("user"))
                ?: throw CommandException(
                        "that user is not in the guild")

        if (roles.size > 1) {
            throw CommandException("mutiple matches for `$roleString`, perhaps try the ID?")
        }

        if (roles.isEmpty()) {
            throw CommandException("no results found")
        }

        val r = roles.first()

        val highestRole = context.member.roles.map { it.position }.max()!!

        val targetHighest = member.roles.map { it.position }.max()!!

        if (r.position >= highestRole)
            throw CommandException("you cannot add roles above yourself")

        if (targetHighest >= highestRole)
            throw CommandException("you cannot execute that on this user")

        if (member.user.id == context.author.id)
            throw CommandException("you cannot execute this on yourself!")

        context.guild.controller.addSingleRoleToMember(member, r).queue()
        context.send().success("Added role **${r.name}** to ${member.user.nameAndDiscrim}",
                true).queue()
    }

    @Command(name = "remove,rem", clearance = CLEARANCE_MOD,
            arguments = ["<user:snowflake>", "<role:string...>"])
    fun removeRole(context: Context, cmdContext: CommandContext) {
        val roleString = cmdContext.get<String>("role")!!

        val roles = findRoles(roleString, context.guild)

        val member = context.guild.getMemberById(cmdContext.get<String>("user"))
                ?: throw CommandException(
                        "that user is not in the guild")

        if (roles.size > 1) {
            throw CommandException("mutiple matches for `$roleString`, perhaps try the ID?")
        }

        if (roles.isEmpty()) {
            throw CommandException("no results found")
        }

        val r = roles.first()

        val highestRole = context.member.roles.map { it.position }.max()!!

        val targetHighest = member.roles.map { it.position }.max()!!

        if (targetHighest >= highestRole)
            throw CommandException("you cannot execute that on this user")

        if (r.position >= highestRole)
            throw CommandException("you cannot remove roles above yourself")

        if (member.user.id == context.author.id)
            throw CommandException("you cannot execute this on yourself!")
        context.guild.controller.removeSingleRoleFromMember(member, r).queue()
        context.send().success("Removed role **${r.name}** to ${member.user.nameAndDiscrim}",
                true).queue()
    }

    private fun findRoles(string: String, guild: Guild): Array<Role> {
        val roles = mutableSetOf<Role>()

        guild.roles.forEach {
            // Match the ID
            if (it.id == string) {
                roles.add(it)
                return@forEach
            }

            // Match the role name exactly
            if (it.name.equals(string, true)) {
                roles.add(it)
                return@forEach
            }

            // match the start of the role
            if (it.name.startsWith(string, true)) {
                roles.add(it)
                return@forEach
            }
        }
        return roles.toTypedArray()
    }
}