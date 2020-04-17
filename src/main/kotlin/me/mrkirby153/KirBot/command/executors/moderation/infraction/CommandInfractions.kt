package me.mrkirby153.KirBot.command.executors.moderation.infraction

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.DB
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.DiscordUser
import me.mrkirby153.KirBot.infraction.Infraction
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.listener.WaitUtils
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import me.mrkirby153.kcutils.utils.TableBuilder
import net.dv8tion.jda.api.Permission
import javax.inject.Inject
import kotlin.system.measureTimeMillis

class CommandInfractions @Inject constructor(private val infractions: Infractions) {

    @Command(name = "infraction", aliases = ["inf"], clearance = CLEARANCE_MOD,
            category = CommandCategory.MODERATION)
    @CommandDescription("Infraction related commands")
    fun inf() {
        // Dummy command to support aliases
    }

    @Command(name = "search", clearance = CLEARANCE_MOD, arguments = ["[query:string...]"],
            parent = "infraction", category = CommandCategory.MODERATION)
    @LogInModlogs
    @CommandDescription("Search for an infraction with the given query")
    @IgnoreWhitelist
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
            val username = users.computeIfAbsent(it.userId) {
                Model.where(DiscordUser::class.java, "id", it).first()?.nameAndDiscrim ?: it
            }
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
            if(!context.channel.checkPermissions(Permission.MESSAGE_ATTACH_FILES)) {
                throw CommandException("The output was too long and I do not have permission to attach files")
            }
            context.channel.sendFile(builtTable.toByteArray(), "infractions.txt").queue()
        }
    }

    @Command(name = "info", clearance = CLEARANCE_MOD, arguments = ["<id:int>"],
            parent = "infraction", category = CommandCategory.MODERATION, permissions = [Permission.MESSAGE_EMBED_LINKS])
    @LogInModlogs
    @CommandDescription("Gets detailed information about an infraction")
    @IgnoreWhitelist
    fun info(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<Int>("id")!!
        val infraction = Model.where(Infraction::class.java, "id", id).first()
        if (infraction == null || infraction.guild != context.guild.id)
            throw CommandException("Infraction not found!")
        val user = Model.where(DiscordUser::class.java, "id", infraction.userId).first()
        val moderator = Model.where(DiscordUser::class.java, "id", infraction.issuerId).first()
        context.send().embed("#${id} - ${infraction.type.internalName.capitalize()}") {
            timestamp {
                millis(infraction.createdAt.time)
            }
            author {
                name = user.nameAndDiscrim
                val jdaUser = Bot.shardManager.getUserById(user.id)
                if(jdaUser != null) {
                    iconUrl = jdaUser.effectiveAvatarUrl
                }
            }
            fields {
                field {
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
                    title = "Moderator"
                    description {
                        if (moderator != null)
                            +moderator.nameAndDiscrim
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
            arguments = ["<id:int>", "[reason:string...]"], category = CommandCategory.MODERATION,
            parent = "infraction", aliases = ["delete"], permissions = [Permission.MESSAGE_ADD_REACTION])
    @LogInModlogs
    @CommandDescription("Clears an infraction (Deletes it from the database)")
    @IgnoreWhitelist
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

        context.channel.sendMessage(
                "Are you sure you want to delete this infraction? This cannot be undone").queue { msg ->
            WaitUtils.confirmYesNo(msg, context.author, {
                infraction.delete()
                context.channel.sendMessage("Infraction `$id` deleted!").queue()
                context.guild.kirbotGuild.logManager.genericLog(LogEvent.ADMIN_COMMAND, ":warning:",
                        "Infraction `$id` deleted by ${context.author.nameAndDiscrim} (`${context.author.id}`): `$reason`")
            }, {
                msg.editMessage("Canceled!").queue()
            })
        }
    }

    @Command(name = "reason", clearance = CLEARANCE_MOD,
            arguments = ["<id:number>", "<reason:string...>"],
            category = CommandCategory.MODERATION, parent = "infraction", aliases = ["update"])
    @LogInModlogs
    @CommandDescription("Sets the reason of an infraction")
    @IgnoreWhitelist
    fun reason(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<Int>("id")!!
        val reason = cmdContext.get<String>("reason")!!

        val infraction = Model.where(Infraction::class.java, "id", id).first()
                ?: throw CommandException("That infraction doesn't exist")

        // Prevent modifying infractions from different guilds
        if (infraction.guild != context.guild.id)
            throw CommandException("That infraction doesn't exist")

        infraction.reason = reason
        infraction.save()

        context.send().success("Updated reason of `$id`").queue()
    }

    @Command(name = "import-banlist", clearance = CLEARANCE_ADMIN,
            category = CommandCategory.MODERATION, parent = "infraction", permissions = [Permission.MESSAGE_ADD_REACTION])
    @LogInModlogs
    @CommandDescription("Imports the banlist as infractions")
    @IgnoreWhitelist
    fun importBanlist(context: Context, cmdContext: CommandContext) {
        context.channel.sendMessage("Are you sure you want to import the banlist?").queue { msg ->
            WaitUtils.confirmYesNo(msg, context.author, {
                context.channel.sendMessage(":timer: Importing from the banlist...").queue {
                    val timeTaken = measureTimeMillis {
                        infractions.importFromBanlist(context.guild.kirbotGuild)
                    }
                    it.editMessage("Completed in ${Time.format(1, timeTaken)}").queue()
                }
            }, {
                msg.editMessage("Canceled!").queue()
            })
        }
    }

    @Command(name = "export", clearance = CLEARANCE_MOD, category = CommandCategory.MODERATION,
            parent = "infraction", permissions = [Permission.MESSAGE_ATTACH_FILES])
    @LogInModlogs
    @IgnoreWhitelist
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
        context.channel.sendMessage("Exported ${infractions.size} infractions").addFile(
                infString.toByteArray(), "infractions.csv").queue()
    }
}