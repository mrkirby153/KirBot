package me.mrkirby153.KirBot.modules

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.guild.SpamSettings
import me.mrkirby153.KirBot.infraction.InfractionType
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.utils.Bucket
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class Spam : Module("spam") {

    private val locks = mutableMapOf<String, Semaphore>()

    private val settingsCache: Cache<String, SpamSettings> = CacheBuilder.newBuilder().expireAfterWrite(
            1, TimeUnit.SECONDS).build()


    val MAX_MESSAGES: Int = 2
    val period = 10

    init {
        dependencies.add(Redis::class.java)
    }

    override fun onLoad() {

    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author.id == event.guild.selfMember.user.id)
            return
        // Acquire a lock for the guild
        getLock(event.guild.id).acquire()
//        Bot.LOG.debug("Checking ${event.author}")
        val effectiveRules = calculateEffectiveRules(event.author, event.guild)

        effectiveRules.forEach { rule ->
            val bucket = Bucket(
                    "${rule.rule.toString().toLowerCase()}:${event.guild.id}:${event.author.id}",
                    rule.count, rule.period * 1000)
            val count = getViolations(rule.rule, event.message.contentRaw)
            if (bucket.check(event.messageId, count)) {
                Bot.LOG.debug("${event.author} has violated ${rule.rule}!")
                violate(event.author, event.guild, rule.rule, rule.punishment, bucket)
            }
        }

        // Unlock the guild
        getLock(event.guild.id).release()
    }

    private fun getLock(guild: String) = this.locks.getOrPut(guild, { Semaphore(1) })


    private fun violate(user: User, guild: Guild, violation: Rule, punishment: Punishment,
                        bucket: Bucket) {
        ModuleManager[Redis::class.java].getConnection().use { con ->
            val key = "lv:${guild.id}:${user.id}"
            val last = (con.get(key) ?: "0").toLong()

            con.setex(key, 60, System.currentTimeMillis().toString())

            if (last + (10 * 1000) < System.currentTimeMillis()) {
                // VIOLATION
                // TODO 4/2/18: Show amount and time
                guild.kirbotGuild.logManager.genericLog(LogEvent.SPAM_VIOLATE, ":helmet_with_cross:",
                        "${user.nameAndDiscrim} (`${user.id}`) Has violated `$violation`: ${violation.friendlyType} (${bucket.count(
                                "")} / ${bucket.size("")}s)")

                when (punishment.type) {
                    InfractionType.MUTE -> {
                        Infractions.mute(user.id, guild, "1", "Spam Detected")
                    }
                    InfractionType.KICK -> Infractions.kick(user.id, guild, "1", "Spam Detected")
                    InfractionType.BAN -> Infractions.ban(user.id, guild, "1", "Spam Detected")
                    InfractionType.TEMPMUTE -> {
                        Infractions.tempMute(user.id, guild, "1", punishment.duration.toLong(),
                                TimeUnit.SECONDS, "Spam Detected")
                    }
                    else -> {
                        Bot.LOG.warn("Unknown punishment ${punishment.type}")
                    }
                }
            } else {
                Bot.LOG.debug("Last violation was within 10 seconds, ignoring")
            }
        }
    }

    private fun calculateEffectiveRules(user: User, guild: Guild): Array<SpamRule> {
        val clearance = user.getClearance(guild)

        // Find the rules with equal or lower clearance
        val settings = getSettings(guild).settings

        val effectiveCats = settings.keySet().map { it.toInt() }.filter { it >= clearance }

        val rules = mutableListOf<SpamRule>()
        effectiveCats.forEach { cat ->
            val obj = settings.getJSONObject(cat.toString())

            // can't use a foreach here because it breaks stuff :meowglare:
            for (i in 0 until Rule.values().size) {
                val it = Rule.values()[i]
                val rule = obj.optJSONObject(it.jsonType) ?: continue

                rules.add(SpamRule(it, rule.getInt("count"), rule.getInt("period"), Punishment(
                        InfractionType.valueOf(obj.getString("punishment")),
                        obj.optInt("punishment_duration"))))
            }
        }
        return rules.toTypedArray()
    }

    private fun getSettings(guild: Guild): SpamSettings {
        val settings = settingsCache.getIfPresent(guild.id)
        return if (settings == null) {
            val s = Model.first(SpamSettings::class.java, guild.id)
                    ?: SpamSettings().apply { this.id = guild.id }
            settingsCache.put(guild.id, s)
            s
        } else {
            settings
        }
    }

    private fun getViolations(rule: Rule, message: String): Int {
        when (rule) {
            Rule.MAX_MESSAGES -> return 1
            Rule.MAX_NEWLINES -> {
                val count = message.split(Regex("\\r\\n|\\r|\\n")).count()
                return if (count > 1) count else 0
            }
            Rule.MAX_MENTIONS -> {
                val pattern = Regex("<@!?\\d+>")

                return pattern.findAll(message).count()
            }
            Rule.MAX_LINKS -> {
                val pattern = Regex("https?://")

                return pattern.findAll(message).count()
            }
        }
    }

    private class SpamRule(val rule: Rule, val count: Int, val period: Int,
                           val punishment: Punishment) {
        override fun toString(): String {
            return "SpamRule(rule=$rule, count=$count, period=$period, punishment=$punishment)"
        }
    }

    // TODO 4/2/18: Add clean duration to clean the past X messages
    private data class Punishment(val type: InfractionType, val duration: Int)

    private enum class Rule(val jsonType: String, val friendlyType: String) {
        MAX_MESSAGES("max_messages", "Too many messages"),
        MAX_NEWLINES("max_newlines", "Too many lines"),
        MAX_MENTIONS("max_mentions", "Too many mentions"),
        MAX_LINKS("max_links", "Too many links")
    }
}