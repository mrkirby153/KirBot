package me.mrkirby153.KirBot.realname

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.server.Server
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.utils.PermissionUtil


class RealnameHandler(var server: Server) {

    @JvmOverloads
    fun updateNames(silent: Boolean = false) {
        val realnameSetting = Database.getRealnameSetting(server) ?: return
        if (realnameSetting == RealnameSetting.OFF) {
            server.members.forEach {
                updateUnidentified(it, false)
                try {
                    server.guild.controller.setNickname(it, null).queue()
                } catch (e: PermissionException) {
                    // Ignore
                }
            }
            return
        }
        val identifiedMembers = mutableListOf<Member>()
        for (member in server.members) {
            if (member.user.isBot)
                continue
            val name = Database.getRealname(realnameSetting == RealnameSetting.FIRST_ONLY, member)
            if (name == null) {
                try {
                    server.guild.controller.setNickname(member, null).queue()
                } catch (e: PermissionException) {
                    // Ignore
                }
                continue
            }
            if (member.effectiveName != name) {
                // Update name
                // Check permission
                val selfMember = server.selfMember
                if (PermissionUtil.checkPermission(server.guild, selfMember, Permission.NICKNAME_MANAGE)) {
                    try {
                        server.guild.controller.setNickname(member, name).queue()
                        updateUnidentified(member, true)
                        identifiedMembers.add(member)
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
            } else {
                updateUnidentified(member, true)
                identifiedMembers.add(member)
            }
        }
        server.members.filter {
            !identifiedMembers.contains(it)
        }.forEach {
            updateUnidentified(it, false)
        }
    }

    fun updateUnidentified(member: Member, identified: Boolean) {
        if (!Database.requireRealname(server)) {
            val repository = server.repository() ?: return
            val unidentified = repository.get(String::class.java, "unidentified-role")
            val id = repository.get(String::class.java, "identified-role")

            if (unidentified != null)
                try {
                    server.roles.first { it.id == unidentified }.delete().queue()
                } catch(e: NoSuchElementException) {
                    // Ignore
                }

            if (id != null)
                try {
                    server.roles.first { it.id == id }.delete().queue()
                } catch(e: NoSuchElementException) {
                    // Ignore
                }

            repository.remove("unidentified-role")
            repository.remove("identified-role")
            return
        }
        if (identified) {
            server.controller.removeRolesFromMember(member, getUnidentifiedRole()).complete(true)
            server.controller.addRolesToMember(member, getIdentifiedRole()).complete(true)
        } else {
            server.controller.removeRolesFromMember(member, getIdentifiedRole()).complete(true)
            server.controller.addRolesToMember(member, getUnidentifiedRole()).complete(true)
        }
    }

    fun getUnidentifiedRole(): Role {
        val roleId = server.repository()?.get(String::class.java, "unidentified-role")
        if (roleId == null) {
            // Create, save, and return the role
            val role = server.controller.createRole().complete(true)
            role.manager.setName("Unidentified").complete(true)
            server.repository()?.put("unidentified-role", role.id)
            return role
        } else {
            val role = server.roles.first { it.id == roleId }
            return role
        }
    }

    fun getIdentifiedRole(): Role {
        val roleId = server.repository()?.get(String::class.java, "identified-role")
        if (roleId == null) {
            // Create, save, and return the role
            val role = server.controller.createRole().complete(true)
            role.manager.setName("Identified").complete(true)
            server.repository()?.put("identified-role", role.id)
            return role
        } else {
            val role = server.roles.first { it.id == roleId }
            return role
        }
    }
}