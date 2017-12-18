package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.RequiresClearance
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.*
import net.dv8tion.jda.core.Permission
import java.util.concurrent.TimeUnit

@Command("permissions")
@RequiresClearance(Clearance.BOT_MANAGER)
class CommandDumpPermissions : BaseCommand(false) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val permissions = mutableMapOf<Permission, Boolean>()
        Permission.values().filter { it != Permission.UNKNOWN }.forEach { p ->
            permissions.put(p, context.channel.checkPermissions(p))
        }
        context.channel.sendMessage(buildString {
            appendln("I have the following permissions in this channel:")
            val maxLength = permissions.keys.map { it.getName().length }.max() ?: 0
            append("```")
            permissions.forEach { permission, status ->
                append(permission.getName())
                append(" ".repeat(Math.max(0, maxLength - permission.getName().length)))
                append(" - ")
                if (status)
                    append(GREEN_CHECK)
                else
                    append(RED_CROSS)
                append("\n")
            }
            append("```")
        }).queue {
            it.deleteAfter(30, TimeUnit.SECONDS)
            context.deleteAfter(30, TimeUnit.SECONDS)
        }
    }
}