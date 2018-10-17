package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.server.BackfillTask
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.kcutils.Time

@Command(name = "backfill", admin = true, arguments = ["<type:string>", "<id:snowflake>"])
class CommandBackfill : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val type = cmdContext.get<String>("type")!!
        val id = cmdContext.get<String>("id")!!

        val backfillType = when (type.toLowerCase()) {
            "guild" -> BackfillTask.BackfillType.GUILD
            "channel" -> BackfillTask.BackfillType.CHANNEL
            "message" -> BackfillTask.BackfillType.MESSAGE
            else -> {
                throw CommandException("Type not found")
            }
        }
        val msg = context.channel.sendMessage(
                "Starting backfill of ${backfillType.name} $id").complete()
        Bot.scheduler.submit(BackfillTask(context.guild, id, backfillType).apply {
            this.callback = {
                msg.editMessage(":ballot_box_with_check: Backfill completed in ${Time.format(0,
                        it.duration)} ${it.created}/${it.updated}/${it.deleted} (${it.scanned})").queue()
            }
        })
    }
}