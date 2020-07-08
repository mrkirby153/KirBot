package com.mrkirby153.kirbot.services.impl

import com.mrkirby153.kirbot.entity.repo.PanelUserRepository
import com.mrkirby153.kirbot.entity.repo.RoleClearanceRepository
import com.mrkirby153.kirbot.services.PermissionService
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

const val DEFAULT_CLEARANCE = 0L
const val DEFAULT_CLEARANCE_MOD = 50L
const val DEFAULT_CLEARANCE_ADMIN = 100L

@Service
class PermissionManager(private val panelUserRepository: PanelUserRepository,
                        private val roleClearanceRepository: RoleClearanceRepository) :
        PermissionService {

    private val log = LogManager.getLogger()

    private var globalAdmins = emptyList<String>()
    private val clearanceOverrides = mutableMapOf<String, Long>()

    override fun getClearance(user: User, guild: Guild): Long {
        if (isClearanceOverridden(user)) {
            return Long.MAX_VALUE
        }
        val member = guild.getMember(user) ?: return DEFAULT_CLEARANCE
        return getClearance(member)
    }

    override fun getClearance(member: Member): Long {
        if (isClearanceOverridden(member.user)) {
            return Long.MAX_VALUE
        }
        // The guild owner has admin permissions by default
        if (member.isOwner)
            return DEFAULT_CLEARANCE_ADMIN
        // Save a database query if the user has no roles
        if (member.roles.isEmpty())
            return DEFAULT_CLEARANCE
        val clearances = roleClearanceRepository.getAllByRoleIdIn(member.roles.map { it.id })
        return clearances.maxBy { it.clearanceLevel }?.clearanceLevel ?: DEFAULT_CLEARANCE
    }

    override fun overrideClearance(user: User, time: Long, unit: TimeUnit): Long {
        val duration = TimeUnit.MILLISECONDS.convert(time, unit)
        log.warn("Overriding clearance for {} for the next {}", user, Time.format(1, duration))
        val endTime = System.currentTimeMillis() + duration
        clearanceOverrides[user.id] = endTime
        return endTime
    }

    override fun clearOverriddenClearance(user: User) {
        clearanceOverrides.remove(user.id)
    }

    override fun isGlobalAdmin(user: User) = globalAdmins.contains(user.id)

    private fun isClearanceOverridden(user: User): Boolean {
        clearanceOverrides.entries.removeIf { it.value < System.currentTimeMillis() }
        return clearanceOverrides[user.id] != null
    }

    @PostConstruct
    fun cacheGlobalAdmins() {
        log.info("Loading global admins")
        globalAdmins = panelUserRepository.getGlobalAdmins().mapNotNull { it.id }.toList()
        log.info("Loaded {} global admins", globalAdmins.size)
    }
}