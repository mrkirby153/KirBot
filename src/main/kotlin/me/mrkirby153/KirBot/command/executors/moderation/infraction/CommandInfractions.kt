package me.mrkirby153.KirBot.command.executors.moderation.infraction

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.infraction.Infraction
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.kcutils.utils.TableBuilder

@Command(name = "infractions,infraction,inf", arguments = ["[user:snowflake]"],
        clearance = CLEARANCE_MOD)
class CommandInfractions : BaseCommand(false, CommandCategory.MODERATION) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        throw CommandException("Please provide a sub-command")
    }

    @Command(name = "search", clearance = CLEARANCE_MOD, arguments = ["<user:snowflake>"])
    fun search(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user") ?: throw CommandException("Specify a user")
        val infractions = Model.get(Infraction::class.java, Pair("user_id", user),
                Pair("guild", context.guild.id))

        val header = arrayOf("ID", "User ID", "Issuer", "Type", "Reason", "Active", "Created At")


        val table = TableBuilder(header)
        infractions.forEach {
            table.addRow(
                    arrayOf(it.id.toString(), it.userId, it.issuerId, it.type.toString(), it.reason,
                            if (it.active) "yes" else "no", it.createdAt.toString()))
        }

        val builtTable = table.buildTable()
        if (builtTable.length < 2000) {
            context.channel.sendMessage("```$builtTable```").queue()
        } else {
            context.channel.sendFile(builtTable.toByteArray(), "infractions-$user.txt").queue()
        }
    }

    @Command(name = "reason", clearance = CLEARANCE_MOD,
            arguments = ["<id:number>", "<reason:string...>"])
    fun reason(context: Context, cmdContext: CommandContext) {
        val id = cmdContext.get<Int>("id")!!
        val reason = cmdContext.get<String>("reason")!!

        val infraction = Model.first(Infraction::class.java, Pair("id", id))
                ?: throw CommandException("That infraction doesn't exist")

        infraction.reason = reason
        infraction.save()

        context.send().success("Updated reason of `$id`").queue()
    }
}