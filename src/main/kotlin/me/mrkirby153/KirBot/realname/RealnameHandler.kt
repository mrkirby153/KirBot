package me.mrkirby153.KirBot.realname

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.server.Server
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.utils.PermissionUtil


class RealnameHandler(var server: Server) {

    @JvmOverloads
    fun updateNames(silent: Boolean = false) {
        val realnameSetting = Database.getRealnameSetting(server)?: return
        if(realnameSetting == RealnameSetting.OFF) {
            server.members.forEach{
                try {
                    server.guild.controller.setNickname(it, null).queue()
                } catch (e: PermissionException){
                    // Ignore
                }
            }
            return
        }
        for (member in server.members) {
            val name = Database.getRealname(realnameSetting == RealnameSetting.FIRST_ONLY, member) ?: continue
            if (member.effectiveName != name) {
                // Update name
                // Check permission
                val selfMember = server.selfMember
                if (PermissionUtil.checkPermission(server.guild, selfMember, Permission.NICKNAME_MANAGE)) {
                    try {
                        server.guild.controller.setNickname(member, name).queue()
                        if (!silent)
                            Bot.LOG.info("Updated nickname for ${member.user.name}")
                    } catch(e: PermissionException) {
                        if (!silent)
                            Bot.LOG.warn("Could not update the nickname of ${member.user.name} perhaps he's the server owner?")
                    }
                } else {
                    if (!silent)
                        Bot.LOG.warn("Missing permission NICKNAME_CHANGE ON ${server.id}")
                }
            }
        }
    }
}