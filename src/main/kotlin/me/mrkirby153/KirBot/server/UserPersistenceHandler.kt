package me.mrkirby153.KirBot.server

import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.guild.GuildMember
import me.mrkirby153.KirBot.database.models.guild.GuildMemberRole
import me.mrkirby153.KirBot.inject.Injectable
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Logger
import me.mrkirby153.KirBot.utils.settings.SettingsRepository
import me.mrkirby153.KirBot.utils.checkPermission
import me.mrkirby153.KirBot.utils.getMember
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.KirBot.utils.settings.GuildSettings
import me.mrkirby153.KirBot.utils.toTypedArray
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent
import javax.inject.Inject
import javax.inject.Singleton

@Injectable
@Singleton
class UserPersistenceHandler @Inject constructor(private val logger: Logger){

    fun restore(user: User, guild: Guild) {
        Bot.LOG.debug("Restoring user $user on $guild")
        val persistenceMode = GuildSettings.userPersistence.get(guild).toInt()
        val mode = Mode.decode(persistenceMode)
        if (Mode.ENABLED !in mode) {
            Bot.LOG.debug("Persistence is disabled.")
            return
        }
        val member = user.getMember(guild) ?: return
        val backup = getBackup(member) ?: return
        var changed = false
        if (member.nickname != backup.nick && guild.checkPermission(
                        Permission.NICKNAME_MANAGE) && Mode.NICK in mode) {
            logger.debouncer.create(GuildMemberUpdateNicknameEvent::class.java,
                    Pair("id", member.user.id))
            member.modifyNickname(backup.nick).queue()
            Bot.LOG.debug("Nickname restored to ${backup.nick}")
            changed = true
        }
        if (Mode.ROLES in mode) {
            val roles = backup.roles.mapNotNull { guild.getRoleById(it) }.filter { role ->
                val persistRoles = GuildSettings.persistRoles.get(guild).toTypedArray(String::class.java)
                if (persistRoles.isEmpty())
                    return@filter true
                role.id in persistRoles
            }
            roles.forEach {
                logger.debouncer.create(GuildMemberRoleAddEvent::class.java, Pair("user", user.id),
                        Pair("role", it.id))
            }
            if (roles.isNotEmpty()) {
                guild.modifyMemberRoles(member, roles).queue()
                changed = true
            }
        }
        if (changed) {
            guild.kirbotGuild.logManager.genericLog(LogEvent.MEMBER_RESTORE, ":helmet_with_cross:",
                    "${user.logName} was restored")
        }
    }

    fun restoreVoiceState(user: User, guild: Guild) {
        Bot.LOG.debug("Restoring voice state for $user on $guild")
        val persistenceMoede = GuildSettings.userPersistence.get(guild).toInt()
        val mode = Mode.decode(persistenceMoede)
        if (Mode.ENABLED !in mode) {
            Bot.LOG.debug("Persistence is disabled.")
            return
        }
        val member = user.getMember(guild) ?: return
        val backup = getBackup(member) ?: return

        if (member.voiceState!!.isGuildDeafened != backup.deafened && Mode.DEAFEN in mode) {
            Bot.LOG.debug("Deafened? ${backup.deafened}")
            guild.deafen(member, backup.deafened).queue()
        }
        if (member.voiceState!!.isGuildMuted != backup.muted && Mode.MUTE in mode) {
            Bot.LOG.debug("Muted? ${backup.muted}")
            guild.mute(member, backup.muted).queue()
        }
    }

    fun createBackup(member: Member) {
        val guildMember = Model.where(GuildMember::class.java, "server_id", member.guild.id).where(
                "user_id", member.user.id).first() ?: GuildMember(member)
        guildMember.user = member.user
        guildMember.nick = member.nickname
        guildMember.deafened = member.voiceState?.isGuildDeafened ?: false
        guildMember.muted = member.voiceState?.isGuildMuted ?: false
        guildMember.save()

        val storedRoles = Model.where(GuildMemberRole::class.java, "server_id",
                member.guild.id).where(
                "user_id", member.user.id).get()

        val toAdd = member.roles.filter { it.id !in storedRoles.map { it.roleId } }
        val toRemove = storedRoles.filter { it.id !in member.roles.map { it.id } }

        toAdd.forEach {
            GuildMemberRole(member, it).save()
        }
        toRemove.forEach { it.delete() }
    }

    fun getBackup(member: Member): UserBackup? {
        val guildMember = Model.where(GuildMember::class.java, "server_id", member.guild.id).where(
                "user_id", member.user.id).first() ?: return null
        val roles = Model.where(GuildMemberRole::class.java, "server_id", member.guild.id).where(
                "user_id", member.user.id).get().map { it.roleId }
        return UserBackup(member, guildMember.nick, guildMember.deafened, guildMember.muted, roles)
    }

    fun deleteBackup(member: Member) {
        val m = Model.where(GuildMember::class.java, "server_id", member.guild.id).where("user_id",
                member.user.id).first()
        if(m != null) {
            if (!m.muted && !m.deafened)
                m.delete()
            else
                Bot.LOG.debug("Not deleting backup of $member because they were muted")
        }
        Model.where(GuildMemberRole::class.java, "user_id", member.user.id).where("server_id",
                member.guild.id).delete()
    }

    data class UserBackup(val member: Member, val nick: String?, val deafened: Boolean,
                          val muted: Boolean, val roles: List<String>)

    enum class Mode {
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