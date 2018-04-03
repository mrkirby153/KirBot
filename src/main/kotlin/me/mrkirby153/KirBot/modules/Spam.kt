package me.mrkirby153.KirBot.modules

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.executors.moderation.infraction.TempMute
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.guild.SpamSettings
import me.mrkirby153.KirBot.infraction.InfractionType
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.utils.Bucket
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
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
                violate(event.author, event.guild, rule.rule, rule.punishment)
            }
        }

        // TODO 3/25/18: If the user has recently violated a check, ignore it to prevent duplicates caused by async

        // Unlock the guild
        getLock(event.guild.id).release()
    }

    private fun getLock(guild: String) = this.locks.getOrPut(guild, { Semaphore(1) })


    private fun violate(user: User, guild: Guild, violation: Rule, punishment: Punishment) {
        ModuleManager[Redis::class.java].getConnection().use { con ->
            val key = "lv:${guild.id}:${user.id}"
            val last = (con.get(key) ?: "0").toLong()

            con.setex(key, 60, System.currentTimeMillis().toString())

            if (last + (10 * 1000) < System.currentTimeMillis()) {
                // VIOLATION
                // TODO 4/2/18: Show amount and time
                guild.kirbotGuild.logManager.genericLog(":helmet_with_cross:",
                        "${user.nameAndDiscrim} (`${user.id}`) Has violated `$violation`")

                val inf = Infractions.createInfraction(user.id, guild, "1", "Spam Detected",
                        punishment.type)
                when (punishment.type) {
                    InfractionType.MUTE -> {
                        Infractions.addMutedRole(user, guild)
                        guild.kirbotGuild.logManager.genericLog(":zipper_mouth:",
                                "${user.nameAndDiscrim} (`${user.id}`) Muted by Automatic (`Spam Detected`)")

                    }
                    InfractionType.KICK -> guild.controller.kick(user.id, "Spam Detected").queue()
                    InfractionType.BAN -> guild.controller.ban(user.id, 0).queue()
                    InfractionType.TEMPMUTE -> {
                        Infractions.addMutedRole(user, guild)
                        ModuleManager[Scheduler::class.java].submit(
                                TempMute.UnmuteScheduler(inf.id.toString(), user.id, guild.id),
                                punishment.duration.toLong(), TimeUnit.SECONDS)
                        guild.kirbotGuild.logManager.genericLog(":zipper_mouth:",
                                "${user.nameAndDiscrim} (`${user.id}`) Temp muted for ${Time.format(
                                        1,
                                        punishment.duration * 1000L)} by `Automatic`")
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

    private enum class Rule(val jsonType: String) {
        MAX_MESSAGES("max_messages"),
        MAX_NEWLINES("max_newlines"),
        MAX_MENTIONS("max_mentions"),
        MAX_LINKS("max_links")
    }
}