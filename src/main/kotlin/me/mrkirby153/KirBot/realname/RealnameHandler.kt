package me.mrkirby153.KirBot.realname

import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.database.Database
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.utils.PermissionUtil


class RealnameHandler(var server: Guild, var serverData: ServerData) {

    @JvmOverloads
    fun updateNames(silent: Boolean = false) {
        val realnameSetting = Database.getRealnameSetting(server) ?: return
        if (realnameSetting == RealnameSetting.OFF) {
            server.members.forEach({
                updateUnidentified(it, false)
                setNickname(server, it, null)
            })
            return
        }
        // Members who have a real name in the database
        val identifiedMemebrs = mutableListOf<Member>()
        for (member in server.members) {
            if (member.user.isBot) {
                // Identify bots
                identifiedMemebrs.add(member)
                continue
            }
            val name = Database.getRealname(realnameSetting == RealnameSetting.FIRST_ONLY, member)
            if (name == null) {
                // reset nickname
                setNickname(server, member, null)
                continue
            }
            identifiedMemebrs.add(member)
            updateUnidentified(member, true)
            if (member.effectiveName != name) {
                setNickname(server, member, name)
            }
        }
        server.members.filter {
            !identifiedMemebrs.contains(it)
        }.forEach {
            updateUnidentified(it, false)
        }
    }

    fun setNickname(server: Guild, member: Member, name: String?) {
        if (PermissionUtil.checkPermission(server, server.selfMember, Permission.NICKNAME_MANAGE))
            try {
                server.controller.setNickname(member, name).queue()
            } catch (e: PermissionException) {
                // Ignore
            }
    }

    fun updateUnidentified(member: Member, identified: Boolean) {
        if (!Database.requireRealname(server)) {
            cleanupRealNameRoles()
            return
        }
        val unidentifiedRole = getUnidentifiedRole()
        val identifiedRole = getIdentifiedRole()
        if (identified) {
            server.controller.removeRolesFromMember(member, unidentifiedRole).queue {
                server.controller.addRolesToMember(member, identifiedRole).queue()
            }
        } else {
            server.controller.removeRolesFromMember(member, identifiedRole).queue {
                server.controller.addRolesToMember(member, unidentifiedRole).queue()
            }
        }

    }

    private fun cleanupRealNameRoles() {
        val repository = serverData.repository
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

    fun getUnidentifiedRole(): Role {
        val roleId = serverData.repository.get(String::class.java, "unidentified-role")
        if (roleId == null) {
            // Create, save, and return the role
            val role = server.controller.createRole().complete(true)
            role.manager.setName("Unidentified").complete(true)
            serverData.repository.put("unidentified-role", role.id)
            return role
        } else {
            val role = server.roles.first { it.id == roleId }
            return role
        }
    }

    fun getIdentifiedRole(): Role {
        val roleId = serverData.repository.get(String::class.java, "identified-role")
        if (roleId == null) {
            // Create, save, and return the role
            val role = server.controller.createRole().complete(true)
            role.manager.setName("Identified").complete(true)
            serverData.repository.put("identified-role", role.id)
            return role
        } else {
            val role = server.roles.first { it.id == roleId }
            return role
        }
    }
}