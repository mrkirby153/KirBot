package me.mrkirby153.KirBot.command.executors.moderation.infraction

import com.mrkirby153.bfs.Tuple
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.DiscordUser
import me.mrkirby153.KirBot.infraction.Infraction
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.KirBot.utils.promptForConfirmation
import me.mrkirby153.kcutils.utils.TableBuilder

@Command(name = "infractions,infraction,inf", arguments = ["[user:snowflake]"],
        clearance = CLEARANCE_MOD)
@LogInModlogs
class CommandInfractions : BaseCommand(false, CommandCategory.MODERATION) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        throw CommandException("Please provide a sub-command")
    }

    @Command(name = "search", clearance = CLEARANCE_MOD, arguments = ["<user:snowflake>"])
    fun search(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user") ?: throw CommandException("Specify a user")
        val infractions = Model.get(Infraction::class.java, Tuple("user_id", user),
                Tuple("guild", context.guild.id))

        val header = arrayOf("ID", "Created", "Type", "User", "Moderator", "Reason", "Active")


        val table = TableBuilder(header)
        val users = mutableMapOf<String, String>()
        infractions.forEach {
            val moderator = if (it.issuerId == null) "Unknown" else users.computeIfAbsent(
                    it.issuerId!!) {
                Model.first(DiscordUser::class.java, "id", it)?.nameAndDiscrim ?: it
            }
            val username = users.computeIfAbsent(it.userId, {
                Model.first(DiscordUser::class.java, "id", it)?.nameAndDiscrim ?: it
            })
            table.addRow(
                    arrayOf(it.id.toString(), it.createdAt.toString(), it.type.toString(), username,
                            moderator, it.reason, if (it.active) "yes" else "no"))
        }

        val builtTable = table.buildTable()
        if (builtTable.length < 2000) {
            context.channel.sendMessage("```$builtTable```").queue()
        } else {
            context.channel.sendFile(builtTable.toByteArray(), "infractions-$user.txt").queue()
        }
    }

    @Command(name = "clear", clearance = CLEARANCE_MOD,
            arguments = ["<id:int>", "[reason:string...]"])
    fun clearInfraction(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<Int>("id")!!
        val reason = cmdContext.get<String>("reason") ?: "No reason specified"

        val infraction = Model.first(Infraction::class.java, "id", id) ?: throw CommandException(
                "Infraction not found")

        if (infraction.issuerId == null || infraction.issuerId != context.author.id) {
            if (context.author.getClearance(context.guild) < CLEARANCE_ADMIN)
                throw CommandException("You do not have permission to clear this infraction")
        }
        promptForConfirmation(context,
                "Are you sure you want to delete this infraction? This cannot be undone", {
            infraction.delete()
            context.send().success("Infraction `$id` cleared!", true).queue()
            context.guild.kirbotGuild.logManager.genericLog(LogEvent.ADMIN_COMMAND, ":warning:",
                    "Infraction `$id` deleted by ${context.author.nameAndDiscrim} (`${context.author.id}`): `$reason`")
            true
        })
    }

    @Command(name = "reason", clearance = CLEARANCE_MOD,
            arguments = ["<id:number>", "<reason:string...>"])
    fun reason(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<Int>("id")!!
        val reason = cmdContext.get<String>("reason")!!

        val infraction = Model.first(Infraction::class.java, Tuple("id", id))
                ?: throw CommandException("That infraction doesn't exist")

        infraction.reason = reason
        infraction.save()

        context.send().success("Updated reason of `$id`").queue()
    }
}