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
        val unidentifiedRole = getUnidentifiedRole()
        val identifiedRole = getIdentifiedRole()

        // Clean up roles if the server doesn't require real names
        if (!Database.requireRealname(server)) {
            cleanupRealNameRoles()
        }

        if (realnameSetting == RealnameSetting.OFF) {
            // Reset
            if (!(serverData.repository.getBoolean("has-reset-names") ?: false)) {
                server.members.forEach {
                    server.controller.removeRolesFromMember(it, unidentifiedRole, identifiedRole)
                    setNickname(it, null)
                }
                serverData.repository.put("has-reset-names", true)
            }
            return
        }
        // Remove the old key
        serverData.repository.remove("has-reset-names")

        server.members.forEach { member ->

            // Identify all bots
            if (member.user.isBot) {
                server.controller.modifyMemberRoles(member, arrayOf(identifiedRole).toList(), arrayOf(unidentifiedRole).toList()).queue()
                return@forEach
            }

            val name = Database.getRealname(realnameSetting == RealnameSetting.FIRST_ONLY, member)
            if(name == null){
                server.controller.modifyMemberRoles(member, arrayOf(unidentifiedRole).toList(), arrayOf(identifiedRole).toList()).queue()
                setNickname(member, null)
                return@forEach
            } else {
                server.controller.modifyMemberRoles(member, arrayOf(identifiedRole).toList(), arrayOf(unidentifiedRole).toList()).queue()
                setNickname(member, name)
                return@forEach
            }
        }
    }

    fun setNickname( member: Member, name: String?) {
        if (PermissionUtil.checkPermission(server, server.selfMember, Permission.NICKNAME_MANAGE))
            try {
                server.controller.setNickname(member, name).queue()
            } catch (e: PermissionException) {
                // Ignore
            }
    }

    fun addRoleToUser(member: Member, role: Role){
        if(member.roles.contains(role))
            return
        server.controller.addRolesToMember(member, role).complete()
    }

    fun removeRoleFromUser(member: Member, role: Role){
        if(!member.roles.contains(role))
            return
        server.controller.removeRolesFromMember(member, role).complete()
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