package me.mrkirby153.KirBot.command.executors.admin

import com.mrkirby153.bfs.sql.DB
import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.SettingsRepository

@Command(name = "dumpSettings", clearance = CLEARANCE_ADMIN)
@CommandDescription("Dump the raw guild settings")
class CommandDumpSettings : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val keys = DB.getFirstColumnValues<String>("SELECT DISTINCT `key` FROM `guild_settings` WHERE guild = ?", context.guild.id)

        var str = "```"
        keys.forEach {
            val setting = SettingsRepository.get(context.guild, it) ?: "<NULL>"
            val toAppend = "$it = $setting\n"
            if(str.length + toAppend.length > 1997) {
                str += "```"
                context.channel.sendMessage(str).complete()
                str = "```"
            } else {
                str += toAppend
            }
        }
        str += "```"
        context.channel.sendMessage(str).queue()
    }
}