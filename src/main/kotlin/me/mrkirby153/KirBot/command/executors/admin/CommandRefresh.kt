package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.RequiresClearance
import me.mrkirby153.KirBot.command.args.Arguments
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.sync

@Command("refresh")
@RequiresClearance(Clearance.SERVER_ADMINISTRATOR)
class CommandRefresh : BaseCommand(CommandCategory.ADMIN, Arguments.string("item")) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val arg = cmdContext.get<String>("item")

        when(arg) {
            "channels" -> {
                PanelAPI.updateChannels(context.guild)
                context.success()
            }
            "all" -> {
                context.guild.sync()
                context.success()
            }
            else -> {
                context.fail()
                context.send().error("Not a valid item to refresh!").queue()
            }
        }
    }
}