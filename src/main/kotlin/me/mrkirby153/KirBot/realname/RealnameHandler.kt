package me.mrkirby153.KirBot.realname

import me.mrkirby153.KirBot.data.ServerData
import me.mrkirby153.KirBot.database.api.GuildSettings
import me.mrkirby153.KirBot.database.api.Realname
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.utils.PermissionUtil


class RealnameHandler(var server: Guild, var serverData: ServerData) {

    @JvmOverloads
    fun updateNames(silent: Boolean = false) {
        GuildSettings.get(server).queue { settings ->

            // Clean up roles if the server doesn't require real names
            if (!settings.requireRealname) {
                cleanupRealNameRoles()
            }

            if (settings.realnameSetting == RealnameSetting.OFF) {
                // Reset
                if (!(serverData.repository.getBoolean("has-reset-names") ?: false)) {
                    server.members.forEach {
                        if (settings.requireRealname)
                            server.controller.removeRolesFromMember(it, getUnidentifiedRole(), getIdentifiedRole())
                        setNickname(it, null)
                    }
                    serverData.repository.put("has-reset-names", true)
                }
                return@queue
            }
            // Remove the old key
            serverData.repository.remove("has-reset-names")

            Realname.get(server.members.map { it.user }).queue { map ->
                map.forEach { user, realname ->
                    if (user.isBot) {
                        if (settings.requireRealname)
                            server.controller.modifyMemberRoles(user.getMember(server), arrayListOf(getIdentifiedRole()), arrayListOf(getUnidentifiedRole()).toList()).queue()
                        return@forEach
                    }

                    if (realname == null) {
                        if (settings.requireRealname)
                            server.controller.modifyMemberRoles(user.getMember(server), arrayListOf(getUnidentifiedRole()), arrayListOf(getIdentifiedRole())).queue()
                        setNickname(user.getMember(server), null)
                    } else {
                        var name: String? = ""
                        when (settings.realnameSetting) {
                            RealnameSetting.FIRST_ONLY -> {
                                name = realname.firstName
                            }
                            RealnameSetting.FIRST_LAST -> {
                                name = "${realname.firstName} ${realname.lastName}"
                            }
                            else -> {
                                name = null
                            }
                        }
                        if (settings.requireRealname)
                            server.controller.modifyMemberRoles(user.getMember(server), arrayListOf(getIdentifiedRole()), arrayListOf(getUnidentifiedRole())).queue()
                        setNickname(user.getMember(server), name)
                    }
                }
            }
        }
    }

    fun setNickname(member: Member, name: String?) {
        if (PermissionUtil.checkPermission(server.selfMember, Permission.NICKNAME_MANAGE))
            try {
                server.controller.setNickname(member, name).queue()
            } catch (e: PermissionException) {
                // Ignore
            }
    }

    fun addRoleToUser(member: Member, role: Role) {
        if (member.roles.contains(role))
            return
        server.controller.addRolesToMember(member, role).complete()
    }

    fun removeRoleFromUser(member: Member, role: Role) {
        if (!member.roles.contains(role))
            return
        server.controller.removeRolesFromMember(member, role).complete()
    }

    private fun cleanupRealNameRoles() {
        val repository = serverData.repository
        val unidentified = repository.get(String::class.java, "unidentified-roleId")
        val id = repository.get(String::class.java, "identified-roleId")

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

        repository.remove("unidentified-roleId")
        repository.remove("identified-roleId")
        return
    }

    fun getUnidentifiedRole(): Role {
        val roleId = serverData.repository.get(String::class.java, "unidentified-roleId")
        if (roleId == null) {
            // Create, save, and return the roleId
            val role = server.controller.createRole().complete(true)
            role.manager.setName("Unidentified").complete(true)
            serverData.repository.put("unidentified-roleId", role.id)
            return role
        } else {
            val role = server.roles.first { it.id == roleId }
            return role
        }
    }

    fun getIdentifiedRole(): Role {
        val roleId = serverData.repository.get(String::class.java, "identified-roleId")
        if (roleId == null) {
            // Create, save, and return the roleId
            val role = server.controller.createRole().complete(true)
            role.manager.setName("Identified").complete(true)
            serverData.repository.put("identified-roleId", role.id)
            return role
        } else {
            val role = server.roles.first { it.id == roleId }
            return role
        }
    }
}