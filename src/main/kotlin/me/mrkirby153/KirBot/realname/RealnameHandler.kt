package me.mrkirby153.KirBot.realname

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Database
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role


class RealnameHandler(var guild: KirBotGuild) {

    fun update(lock: Boolean = true) {
        if (lock)
            guild.lock()
        Bot.LOG.debug("Updating nicknames on ${guild.id}")
        val settings = guild.settings
        if (!settings.requireRealname) {
            cleanupRealnameRoles() // Clean up the roles if the server doesn't require real names
        }

        if (settings.realname == RealnameSetting.OFF) {
            var hasReset = guild.extraData.optBoolean("hasResetNames", false)
            if (!hasReset) {
                guild.members.forEach {
                    setNickname(it, null)
                }
                hasReset = true
            }
            guild.extraData.put("hasResetNames", hasReset)
            guild.saveData()
            return
        }

        guild.extraData.remove("hasResetNames")?.let { guild.saveData() }

        val query = "SELECT `id`, `first_name`, `last_name`, CONCAT(`first_name`, ' ', `last_name`) AS 'combined' FROM `user_info` WHERE `id` IN (${guild.members.joinToString(
                ", ") { "'${it.user.id}'" }})"
        val realname = mutableMapOf<String, Triple<String, String, String>>()
        ModuleManager[Database::class].database.getConnection().use { connection ->
            connection.prepareStatement(query).use { ps ->
                ps.executeQuery().use { rs ->
                    println(rs)
                    while (rs.next()) {
                        realname.put(rs.getString("id"),
                                Triple(rs.getString("first_name"), rs.getString("last_name"),
                                        rs.getString("combined")))
                    }
                }
            }
        }

        guild.members.forEach { member ->
            val user = member.user
            val name = realname[user.id]
            if (user.isBot) { // All bots are automatically identified
                if (settings.requireRealname) {
                    guild.controller.modifyMemberRoles(member,
                            arrayListOf(getIdentifiedRole()),
                            arrayListOf(getUnidentifiedRole())).queue()
                }
                return@forEach
            }

            if (name == null) {
                if (settings.requireRealname) {
                    guild.controller.modifyMemberRoles(user.getMember(guild),
                            arrayListOf(getUnidentifiedRole()),
                            arrayListOf(getIdentifiedRole())).queue()
                    setNickname(user.getMember(guild), null)
                }
            } else {
                val nickName: String? = when (settings.realname) {
                    RealnameSetting.FIRST_ONLY -> {
                        name.first
                    }
                    RealnameSetting.FIRST_LAST -> {
                        name.third
                    }
                    else -> {
                        null
                    }
                }
                if (settings.requireRealname) {
                    guild.controller.modifyMemberRoles(user.getMember(guild),
                            arrayListOf(getIdentifiedRole()),
                            arrayListOf(getUnidentifiedRole())).queue()
                }
                setNickname(user.getMember(guild), nickName)
            }
        }
        if (lock)
            guild.unlock()
    }

    private fun setNickname(member: Member, name: String?) {
        if (!guild.selfMember.canInteract(member)) {
            Bot.LOG.debug("Cannot interact with $member. Not changing nickname")
            return
        }
        Bot.LOG.debug(
                "Updating nickname of ${member.user.name}#${member.user.discriminator} to $name")
        guild.controller.setNickname(member, name).queue()
    }

    private fun getUnidentifiedRole(): Role {
        val roleId = guild.extraData.optString("unidentifiedRoleId")
        val role = getOrCreateRole(roleId, "Unidentified")
        if (role.id != roleId) {
            guild.extraData.put("unidentifiedRoleId", role.id)
            guild.saveData()
        }
        return role
    }

    private fun cleanupRealnameRoles() {
        val unidentified = guild.extraData.optString("unidentifiedRoleId")
        val identified = guild.extraData.optString("identifiedRoleId")
        if (unidentified != null) {
            Bot.LOG.debug("Cleaning up unidentified role on $guild")
            guild.roles.firstOrNull { it.id == unidentified }?.delete()?.queue()
            guild.extraData.remove("unidentifiedRoleId")
            guild.saveData()
        }
        if (identified != null) {
            Bot.LOG.debug("Cleaning up identified role on $guild")
            guild.roles.firstOrNull { it.id == identified }?.delete()?.queue()
            guild.extraData.remove("identifiedRoleId")
            guild.saveData()
        }
    }

    private fun getIdentifiedRole(): Role {
        val roleId = guild.extraData.optString("identifiedRoleId")
        val role = getOrCreateRole(roleId, "Identified")
        if (role.id != roleId) {
            guild.extraData.put("identifiedRoleId", role.id)
            guild.saveData()
        }
        return role
    }

    private fun getOrCreateRole(id: String?, name: String): Role {
        return if (id != null) {
            guild.roles.firstOrNull { it.id == id } ?: getOrCreateRole(null, name)
        } else {
            val role = guild.controller.createRole().complete()
            role.manager.setName(name).queue()
            role
        }
    }
}