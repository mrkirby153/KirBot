package me.mrkirby153.KirBot.command.executors.moderation.infraction

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.infraction.Infraction
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.kcutils.utils.TableBuilder

@Command(name = "infractions", arguments = ["[user:snowflake]"], clearance = Clearance.BOT_MANAGER)
class CommandInfractions : BaseCommand(false, CommandCategory.MODERATION) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<String>("user") ?: throw CommandException("Specify a user")
        val infractions = Model.get(Infraction::class.java, Pair("user_id", user),
                Pair("guild", context.guild.id))

        val header = arrayOf("id", "user_id", "issuer", "type", "reason", "active", "created_at",
                "revoked_at")


        val table = TableBuilder(header)
        infractions.forEach {
            table.addRow(
                    arrayOf(it.id.toString(), it.userId, it.issuerId, it.type.toString(), it.reason,
                            it.active.toString(), it.createdAt.toString(), it.revokedAt.toString()))
        }

        context.channel.sendMessage("```${table.buildTable()}```").queue()
    }
}