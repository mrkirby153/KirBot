package me.mrkirby153.KirBot.command.executors.moderation.infraction

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.DB
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

    @Command(name = "search", clearance = CLEARANCE_MOD, arguments = ["<query:string...>"])
    fun search(context: Context, cmdContext: CommandContext) {
        val query = cmdContext.get<String>("query")!!

        val infractions = DB.getResults(
                "SELECT DISTINCT * FROM infractions WHERE `guild` = ? OR (`user_id` = ? AND `guild` = ?) OR (`issuer` = ? AND `guild` = ?) OR (`id` = ?) OR (`reason` LIKE ? AND `guild` = ?)",
                context.guild.id, query, context.guild.id, query, context.guild.id, query, "%$query%", context.guild.id).map {
            val inf = Infraction()
            inf.setData(it)
            return@map inf
        }

        val header = arrayOf("ID", "Created", "Type", "User", "Moderator", "Reason", "Active")


        val table = TableBuilder(header)
        val users = mutableMapOf<String, String>()
        infractions.forEach {
            val moderator = if (it.issuerId == null) "Unknown" else users.computeIfAbsent(
                    it.issuerId!!) {
                Model.where(DiscordUser::class.java, "id", it).first()?.nameAndDiscrim ?: it
            }
            val username = users.computeIfAbsent(it.userId, {
                Model.where(DiscordUser::class.java, "id", it).first()?.nameAndDiscrim ?: it
            })
            table.addRow(
                    arrayOf(it.id.toString(), it.createdAt.toString(), it.type.toString(), username,
                            moderator, it.reason, if (it.active) "yes" else "no"))
        }

        val builtTable = table.buildTable()
        if (builtTable.length < 2000) {
            context.channel.sendMessage("```$builtTable```").queue()
        } else {
            context.channel.sendFile(builtTable.toByteArray(), "infractions.txt").queue()
        }
    }

    @Command(name = "clear", clearance = CLEARANCE_MOD,
            arguments = ["<id:int>", "[reason:string...]"])
    fun clearInfraction(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<Int>("id")!!
        val reason = cmdContext.get<String>("reason") ?: "No reason specified"

        val infraction = Model.where(Infraction::class.java, "id", id).first() ?: throw CommandException(
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

        val infraction = Model.where(Infraction::class.java, "id", id).first()
                ?: throw CommandException("That infraction doesn't exist")

        infraction.reason = reason
        infraction.save()

        context.send().success("Updated reason of `$id`").queue()
    }
}