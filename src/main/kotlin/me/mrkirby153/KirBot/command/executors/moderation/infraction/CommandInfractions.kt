package me.mrkirby153.KirBot.command.executors.moderation.infraction

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.DB
import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.DiscordUser
import me.mrkirby153.KirBot.infraction.Infraction
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.KirBot.utils.promptForConfirmation
import me.mrkirby153.kcutils.utils.TableBuilder
import net.dv8tion.jda.core.MessageBuilder

@Command(name = "infractions,infraction,inf", arguments = ["[user:snowflake]"],
        clearance = CLEARANCE_MOD)
@LogInModlogs
@CommandDescription("Infraction related commands")
class CommandInfractions : BaseCommand(false, CommandCategory.MODERATION) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        throw CommandException("Please provide a sub-command")
    }

    @Command(name = "search", clearance = CLEARANCE_MOD, arguments = ["[query:string...]"])
    @LogInModlogs
    @CommandDescription("Search for an infraction with the given query")
    fun search(context: Context, cmdContext: CommandContext) {
        val query = cmdContext.get<String>("query") ?: ""

        val infractions = DB.getResults(
                "SELECT DISTINCT * FROM infractions WHERE (`user_id` = ? AND `guild` = ?) OR (`issuer` = ? AND `guild` = ?) OR (`id` = ?) OR (`reason` LIKE ? AND `guild` = ?) ORDER BY `id` DESC LIMIT 5",
                query, context.guild.id, query, context.guild.id, query,
                "%$query%", context.guild.id).map {
            val inf = Infraction()
            inf.setData(it)
            return@map inf
        }

        val header = arrayOf("ID", "Created", "Type", "User", "Moderator", "Reason", "Active",
                "Expires")


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
            val reason = if (it.reason != null) if (it.reason!!.length >= 256) it.reason!!.substring(
                    0..255) + "..." else it.reason else ""
            table.addRow(
                    arrayOf(it.id.toString(), it.createdAt.toString(), it.type.toString(), username,
                            moderator, reason, if (it.active) "yes" else "no",
                            it.expiresAt?.toString() ?: "NULL"))
        }

        val builtTable = table.buildTable()
        if (builtTable.length < 2000) {
            context.channel.sendMessage("```$builtTable```").queue()
        } else {
            context.channel.sendFile(builtTable.toByteArray(), "infractions.txt").queue()
        }
    }

    @Command(name = "info", clearance = CLEARANCE_MOD, arguments = ["<id:int>"])
    @LogInModlogs
    @CommandDescription("Gets detailed information about an infraction")
    fun info(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<Int>("id")!!
        val infraction = Model.where(Infraction::class.java, "id", id).first()
        if (infraction == null || infraction.guild != context.guild.id)
            throw CommandException("Infraction not found!")
        context.send().embed(infraction.type.internalName.capitalize()) {
            fields {
                field {
                    val user = Model.where(DiscordUser::class.java, "id", infraction.userId).first()
                    title = "User"
                    description {
                        if (user != null)
                            +user.nameAndDiscrim
                        else
                            +infraction.userId
                    }
                    inline = true
                }
                field {
                    val user = Model.where(DiscordUser::class.java, "id",
                            infraction.issuerId).first()
                    title = "Moderator"
                    description {
                        if (user != null)
                            +user.nameAndDiscrim
                        else
                            +infraction.userId
                    }
                    inline = true
                }
                field {
                    title = "Reason"
                    description = infraction.reason ?: ""
                }
            }
        }.rest().queue()
    }

    @Command(name = "clear", clearance = CLEARANCE_MOD,
            arguments = ["<id:int>", "[reason:string...]"])
    @LogInModlogs
    @CommandDescription("Clears an infraction (Deletes it from the database)")
    fun clearInfraction(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<Int>("id")!!
        val reason = cmdContext.get<String>("reason") ?: "No reason specified"

        val infraction = Model.where(Infraction::class.java, "id", id).first()
                ?: throw CommandException(
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
    @LogInModlogs
    @CommandDescription("Sets the reason of an infraction")
    fun reason(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<Int>("id")!!
        val reason = cmdContext.get<String>("reason")!!

        val infraction = Model.where(Infraction::class.java, "id", id).first()
                ?: throw CommandException("That infraction doesn't exist")

        infraction.reason = reason
        infraction.save()

        context.send().success("Updated reason of `$id`").queue()
    }

    @Command(name = "import-banlist", clearance = CLEARANCE_ADMIN)
    @LogInModlogs
    @CommandDescription("Imports the banlist as infractions")
    fun importBanlist(context: Context, cmdContext: CommandContext) {
        promptForConfirmation(context, "Are you sure you want to import the banlist?", onConfirm = {
            context.channel.sendMessage(":timer: Importing from the banlist...").queue {
                Infractions.importFromBanlist(context.guild.kirbotGuild)
                it.editMessage("Completed!").queue()
            }
            return@promptForConfirmation true
        })
    }

    @Command(name = "export", clearance = CLEARANCE_MOD)
    @LogInModlogs
    @CommandDescription("Exports a CSV of infractions")
    fun export(context: Context, cmdContext: CommandContext) {
        val infractions = Model.where(Infraction::class.java, "guild", context.guild.id).get()
        val infString = buildString {
            appendln("id,user,issuer,reason,timestamp")
            infractions.forEach {
                append(it.id)
                append(",")
                append(it.userId)
                append(",")
                append(it.issuerId)
                append(",")
                if (it.reason?.contains(",") == true) {
                    append("\"")
                    append(it.reason)
                    append("\"")
                } else {
                    append(it.reason ?: "NULL")
                }
                append(",")
                appendln(it.createdAt.toString())
            }
        }
        context.channel.sendFile(infString.toByteArray(), "infractions.csv", MessageBuilder().apply {
            setContent("Exported ${infractions.size} infractions")
        }.build()).queue()
    }
}