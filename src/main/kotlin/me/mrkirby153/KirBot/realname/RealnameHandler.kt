package me.mrkirby153.KirBot.realname

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.api.GuildSettings
import me.mrkirby153.KirBot.database.api.Realname
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Role


class RealnameHandler(var guild: KirBotGuild) {

    fun update() {
        Bot.LOG.debug("Updating nicknames on ${guild.id}")
        GuildSettings.get(guild).queue { settings ->
            if (!settings.requireRealname) {
                cleanupRealnameRoles() // Clean up the roles if the server doesn't require real names
            }

            if (settings.realnameSetting == RealnameSetting.OFF) {
                if (!guild.extraData.optBoolean("hasResetNames", false)) {
                    guild.members.forEach {
                        setNickname(it, null)
                    }
                    guild.extraData.put("hasResetNames", true)
                    guild.saveData()
                }
                return@queue
            }

            guild.extraData.remove("hasResetNames")?.let { guild.saveData() }

            Realname.get(guild.members.map { it.user }).queue { map ->
                map.forEach { user, realname ->
                    if (user.isBot) { // All bots are automatically identified
                        if (settings.requireRealname) {
                            guild.controller.modifyMemberRoles(user.getMember(guild),
                                    arrayListOf(getIdentifiedRole()),
                                    arrayListOf(getUnidentifiedRole())).queue()
                        }
                        return@forEach
                    }

                    if (realname == null) {
                        if (settings.requireRealname) {
                            guild.controller.modifyMemberRoles(user.getMember(guild),
                                    arrayListOf(getUnidentifiedRole()),
                                    arrayListOf(getIdentifiedRole())).queue()
                            setNickname(user.getMember(guild), null)
                        }
                    } else {
                        var name: String? = null
                        name = when (settings.realnameSetting) {
                            RealnameSetting.FIRST_ONLY -> {
                                realname.firstName
                            }
                            RealnameSetting.FIRST_LAST -> {
                                "${realname.firstName} ${realname.lastName}"
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
                        setNickname(user.getMember(guild), name)
                    }
                }
            }
        }
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