package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.GREEN_CHECK
import me.mrkirby153.KirBot.utils.RED_CROSS
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.deleteAfter
import net.dv8tion.jda.core.Permission
import java.util.concurrent.TimeUnit


class CommandDumpPermissions {

    @Command(name = "permissions", clearance = CLEARANCE_ADMIN)
    @CommandDescription("Displays all the permissions the bot currently has in the current channel")
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val permissions = mutableMapOf<Permission, Boolean>()
        Permission.values().filter { it != Permission.UNKNOWN }.forEach { p ->
            permissions[p] = context.channel.checkPermissions(p)
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