package me.mrkirby153.KirBot.server

import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.guild.GuildMember
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Logger
import me.mrkirby153.KirBot.utils.SettingsRepository
import me.mrkirby153.KirBot.utils.checkPermission
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.KirBot.utils.toTypedArray
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent

object UserPersistenceHandler {

    fun restore(user: User, guild: Guild) {
        Bot.LOG.debug("Restoring user $user on $guild")
        val persistenceMoede = SettingsRepository.get(guild, "user_persistence", "0")!!.toInt()
        val mode = Mode.decode(persistenceMoede)
        if (Mode.ENABLED !in mode) {
            Bot.LOG.debug("Persistence is disabled.")
            return
        }
        val member = user.getMember(guild) ?: return
        val settings = Model.query(GuildMember::class.java).where("user_Id",
                user.id).where("server_id",
                guild.id).first() ?: return
        val logger = ModuleManager[Logger::class.java]
        var changed = false
        if (member.nickname != settings.nick && guild.checkPermission(
                        Permission.NICKNAME_MANAGE) && Mode.NICK in mode) {
            logger.debouncer.create(GuildMemberNickChangeEvent::class.java,
                    Pair("id", member.user.id))
            guild.controller.setNickname(member, settings.nick).queue()
            Bot.LOG.debug("Nickname restored to ${settings.nick}")
            changed = true
        }

        // Restore roles
        if (Mode.ROLES in mode) {
            val rolesToRestore = settings.roles.filter { role ->
                val persistRoles = SettingsRepository.getAsJsonArray(guild,
                        "persist_roles")?.toTypedArray(String::class.java) ?: emptyList()
                if (persistRoles.isEmpty())
                    return@filter true
                role.role != null && role.role!!.id in persistRoles
            }
            rolesToRestore.forEach {
                logger.debouncer.create(GuildMemberRoleAddEvent::class.java, Pair("user", user.id),
                        Pair("role", it.role!!.id))
            }
            Bot.LOG.debug("Restoring ${rolesToRestore.size} roles")
            guild.controller.addRolesToMember(member,
                    rolesToRestore.asSequence().map { it.role }.filterNotNull().toList()).queue()
            if (rolesToRestore.isNotEmpty())
                changed = true
        }

        if (changed) {
            guild.kirbotGuild.logManager.genericLog(LogEvent.MEMBER_RESTORE, ":helmet_with_cross:",
                    "${user.logName} was restored")
        }
    }

    fun restoreVoiceState(user: User, guild: Guild) {
        Bot.LOG.debug("Restoring voice state for $user on $guild")
        val persistenceMoede = SettingsRepository.get(guild, "user_persistence", "0")!!.toInt()
        val mode = Mode.decode(persistenceMoede)
        if (Mode.ENABLED !in mode) {
            Bot.LOG.debug("Persistence is disabled.")
            return
        }
        val member = user.getMember(guild) ?: return
        val settings = Model.where(GuildMember::class.java, "user_Id", user.id).where("server_id",
                guild.id).first() ?: return

        if (member.voiceState.isGuildDeafened != settings.deafened && Mode.DEAFEN in mode) {
            Bot.LOG.debug("Deafened? ${settings.deafened}")
            guild.controller.setDeafen(member, settings.deafened).queue()
        }
        if (member.voiceState.isGuildMuted != settings.muted && Mode.MUTE in mode) {
            Bot.LOG.debug("Muted? ${settings.muted}")
            guild.controller.setMute(member, settings.muted).queue()
        }
    }

    private enum class Mode {
        ENABLED,
        MUTE,
        DEAFEN,
        NICK,
        ROLES;

        val perm = 1.shl(this.ordinal)

        companion object {
            fun decode(bitField: Int): List<Mode> {
                val list = mutableListOf<Mode>()
                Mode.values().forEach { m ->
                    if (bitField.and(m.perm) > 0)
                        list.add(m)
                }
                return list
            }
        }
    }
}