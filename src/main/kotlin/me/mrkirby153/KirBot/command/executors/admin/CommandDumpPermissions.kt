package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.utils.PermissionUtil
import java.awt.Color
import java.util.concurrent.TimeUnit

class CommandDumpPermissions : CmdExecutor() {


    override fun execute(context: Context, cmdContext: CommandContext) {
        val permissions = mutableMapOf<Permission, Boolean>()
        Permission.values().forEach { p ->
            permissions.put(p, PermissionUtil.checkPermission(context.channel as Channel, context.member, p))
        }
        context.send().embed("Permissions"){
            setColor(Color.GREEN)
            setDescription("I have the following permissions in this channel")
            permissions.forEach{
                addField(it.key.getName(), it.value.toString(), true)
            }
        }.rest().queue {
            it.delete().queueAfter(30, TimeUnit.SECONDS)
        }
    }
}