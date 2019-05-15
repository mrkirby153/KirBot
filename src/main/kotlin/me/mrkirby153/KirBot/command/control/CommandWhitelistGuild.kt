package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.kcutils.Time
import java.text.SimpleDateFormat

@Command(name = "whitelistGuild", admin = true, arguments = ["<guild:string>"])
class CommandWhitelistGuild : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!Bot.properties.getOrDefault("guild-whitelist", "false").toString().toBoolean()) {
            context.send().info(":warning: The guild whitelist is disabled!").queue()
            return
        }
        val guild = cmdContext.get<String>("guild")!!

        ModuleManager[Redis::class.java].getConnection().use { con ->
            con.setex("whitelist:$guild", 3600 * 24, "true")
            val time = System.currentTimeMillis() + ((3600 * 24) * 1000)

            context.send().success(
                    "Whitelisted `$guild` for 24 hours. (Expires at ${SimpleDateFormat(
                            Time.DATE_FORMAT_NOW).format(time)})").queue()
        }
    }
}