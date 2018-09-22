package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.kirbotGuild

@Command(name = "guild-backfill", admin = true, arguments = ["[guild:string]"])
class CommandBackfillGuild : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val guildId = cmdContext.get<String>("guild") ?: context.guild.id

        val guild = Bot.shardManager.getGuild(guildId) ?: throw CommandException("Guild not found!")
        val msg = context.channel.sendMessage(
                "Starting backfill on ${guild.name} (`${guild.id}`)").complete()

        guild.kirbotGuild.completeBackfill {
            msg.editMessage(
                    ":ballot_box_with_check: Backfill completed ```\nCreated: ${it.created}\nUpdated: ${it.updated}\nDeleted: ${it.deleted}```").queue()
        }
    }
}