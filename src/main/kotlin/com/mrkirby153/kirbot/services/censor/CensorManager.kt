package com.mrkirby153.kirbot.services.censor

import com.mrkirby153.kirbot.services.PermissionService
import com.mrkirby153.kirbot.services.setting.GuildSettings
import com.mrkirby153.kirbot.services.setting.SettingsService
import com.mrkirby153.kirbot.utils.checkPermissions
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import org.apache.logging.log4j.LogManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class CensorManager(
        private val settingsService: SettingsService,
        private val eventPublisher: ApplicationEventPublisher,
        private val permissionService: PermissionService
) : CensorService {

    private val log = LogManager.getLogger()

    private val censorRules = listOf(
            ZalgoRule(), TokenRule(), WordRule(), InviteRule(), DomainRule()
    )

    override fun getRules(guild: Guild): List<CensorSetting> {
        return settingsService.getSetting(GuildSettings.censorSettings, guild)?.toList()
                ?: emptyList()
    }

    override fun getEffectiveRules(guild: Guild, user: User): List<CensorSetting> {
        val member = guild.getMember(user) ?: return emptyList()
        val userClearance = permissionService.getClearance(member)
        return getRules(guild).filter { it.level >= userClearance }
    }

    override fun check(msg: Message): List<ViolationException> {
        log.debug("Checking message ${msg.contentRaw}")
        val violations = mutableListOf<ViolationException>()
        val rules = getEffectiveRules(msg.guild, msg.author)
        rules.forEach { rule ->
            censorRules.forEach {
                try {
                    it.check(msg, rule)
                } catch (e: ViolationException) {
                    violations.add(e)
                }
            }
        }
        log.debug("Message ${msg.contentRaw} has ${violations.size} violations")
        return violations
    }

    private fun checkViolations(message: Message) {
        val violations = check(message)
        if (violations.isNotEmpty()) {
            var deleted = false
            if (message.channel.checkPermissions(Permission.MESSAGE_MANAGE)) {
                deleted = true
                message.delete().queue()
            } else {
                log.debug(
                        "Could not delete $message in ${message.channel}. Lacking MESSAGE_MANAGE")
            }
            log.debug("Censoring message ${message.contentRaw}")
            eventPublisher.publishEvent(MessageCensorEvent(message, violations, deleted))
        }
    }

    @EventListener
    @Async
    fun onMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author == event.jda.selfUser)
            return // Don't check ourselves
        checkViolations(event.message)
    }

    @EventListener
    @Async
    fun onMessageEdit(event: GuildMessageUpdateEvent) {
        if (event.author == event.jda.selfUser)
            return // Don't check ourselves
        checkViolations(event.message)
    }
}