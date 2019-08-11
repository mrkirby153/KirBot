package me.mrkirby153.KirBot.modules

import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.utils.Bucket
import me.mrkirby153.KirBot.utils.EMOJI_RE
import me.mrkirby153.KirBot.utils.SettingsRepository
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.isNumber
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.logName
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import org.json.JSONObject
import java.sql.Timestamp
import java.time.Instant
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class Spam : Module("spam") {

    private val locks = mutableMapOf<String, Semaphore>()

    init {
        dependencies.add(Redis::class.java)
    }

    override fun onLoad() {

    }

    @Subscribe
    fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author.id == event.guild.selfMember.user.id)
            return // Ignore ourselves
        if (event.guild.id !in locks.keys)
            locks[event.guild.id] = Semaphore(1)
        locks[event.guild.id]!!.acquire()

        val rules = getEffectiveRules(event.guild, event.author)

        try {
            rules.forEach {
                checkMessage(event.message, it)
            }
        } catch (e: ViolationException) {
            violate(e)
        }
        locks[event.guild.id]!!.release()
    }


    private fun checkMessage(message: Message, level: Int) {
        fun checkBucket(check: String, friendlyText: String, amount: Int) {
            if (amount == 0)
                return
            val bucket = getBucket(check, message.guild.id, level) ?: return
            if (bucket.check(message.author.id, amount)) {
                throw ViolationException(check.toUpperCase(), "$friendlyText (${bucket.count(
                        message.author.id)}/${bucket.size(message.author.id)}s)", message.author,
                        level, message.guild, message.channel)
            }
        }
        checkBucket("max_messages", "Too many messages", 1)
        checkBucket("max_newlines", "Too many newlines",
                message.contentRaw.split(Regex("\\r\\n|\\r|\\n")).count())
        checkBucket("max_mentions", "Too many mentions", {
            val pattern = Regex("<@[!&]?\\d+>")
            pattern.findAll(message.contentRaw).count()
        }.invoke())
        checkBucket("max_links", "Too many links", {
            val pattern = Regex("https?://")
            pattern.findAll(message.contentRaw).count()
        }.invoke())
        checkBucket("max_emoji", "Too many emoji", {
            EMOJI_RE.findAll(message.contentRaw).count()
        }.invoke())
        checkBucket("max_uppercase", "Too many uppercase", {
            val pattern = Regex("[A-Z]")
            pattern.findAll(message.contentRaw).count()
        }.invoke())
        checkBucket("max_attachments", "Too many attachments", message.attachments.size)
        checkDuplicates(message, level)
    }

    private fun checkDuplicates(message: Message, level: Int) {
        val rule = getRule(message.guild.id, level) ?: return
        val dupeSettings = rule.optJSONObject("max_duplicates") ?: return
        val count = dupeSettings.getInt("count")
        val period = dupeSettings.getInt("period")

        ModuleManager[Redis::class.java].getConnection().use { jedis ->
            val key = "spam:duplicates:${message.guild.id}:${message.author.id}:${message.contentRaw}"
            val n = jedis.incr(key)
            if (n == 1L) {
                jedis.expire(key, period)
            }
            if (n > count) {
                throw ViolationException("MAX_DUPLICATES", "Too many duplicates ($n)",
                        message.author, level, message.guild, message.channel)
            }
        }
    }

    private fun violate(violation: ViolationException) {
        ModuleManager[Redis::class.java].getConnection().use { con ->
            val key = "lv:${violation.guild.id}:${violation.user.id}"
            val last = (con.get(key) ?: "0").toLong()
            con.setex(key, 60, System.currentTimeMillis().toString())
            if (last + (10 * 1000) < System.currentTimeMillis()) {
                val settings = getSettings(violation.guild.id)
                val rule = getRule(violation.guild.id, violation.level)
                val punishment = rule?.optString("punishment", settings.optString("punishment"))
                        ?: "NONE"
                val duration = rule?.optLong("punishment_duration",
                        settings.optLong("punishment_duration", -1)) ?: -1

                // Modlog
                violation.guild.kirbotGuild.logManager.genericLog(LogEvent.SPAM_VIOLATE,
                        ":helmet_with_cross:",
                        "${violation.user.logName} Has violated ${violation.type} in <#${violation.channel.id}>: ${violation.message}")

                val reason = "Spam detected: ${violation.type} in #${violation.channel.name}: ${violation.msg}"
                when (punishment.toUpperCase()) {
                    "NONE" -> {
                        // Do nothing
                    }
                    "MUTE" -> {
                        Infractions.mute(violation.user.id, violation.guild,
                                violation.guild.selfMember.user.id,
                                reason)
                    }
                    "KICK" -> {
                        Infractions.kick(violation.user.id, violation.guild,
                                violation.guild.selfMember.user.id,
                                reason)
                    }
                    "BAN" -> {
                        Infractions.ban(violation.user.id, violation.guild,
                                violation.guild.selfMember.user.id,
                                reason)
                    }
                    "TEMPMUTE" -> {
                        Infractions.tempMute(violation.user.id, violation.guild,
                                violation.guild.selfMember.user.id,
                                duration, TimeUnit.SECONDS, reason)
                    }
                    "TEMPBAN" -> {
                        Infractions.tempban(violation.user.id, violation.guild,
                                violation.guild.selfMember.user.id, duration, TimeUnit.SECONDS,
                                reason)
                    }
                    else -> {
                        Bot.LOG.warn("Unknown punishment $punishment on guild ${violation.guild}")
                        violation.guild.kirbotGuild.logManager.genericLog(LogEvent.SPAM_VIOLATE,
                                ":warning:",
                                "Unknown punishment `${punishment.toUpperCase()}`. No action has been taken")
                    }
                }

                if (settings.has("clean_count") || settings.has("clean_duration") || rule?.has(
                                "clean_count") == true || rule?.has("clean_duration") == true) {
                    Bot.LOG.debug("Performing clean")
                    val cleanCount = rule?.optString("clean_count",
                            settings.optString("clean_count", null))
                    val cleanDuration = rule?.optString("clean_duration",
                            settings.optString("clean_duration", null))
                    Thread.sleep(
                            250) // Wait to make sure in-flight stuff has been committed to the db
                    val messageQuery = Model.query(GuildMessage::class.java).where("author",
                            violation.user.id).where("server_id", violation.guild.id).where(
                            "deleted", false)
                    if (cleanCount != null) {
                        messageQuery.limit(cleanCount.toLong())
                    }
                    if (cleanDuration != null) {
                        val instant = Instant.now().minusSeconds(cleanDuration.toLong())
                        val after = Timestamp(instant.toEpochMilli())
                        messageQuery.where("created_at", ">", after)
                    }
                    val messages = messageQuery.get()
                    Bot.LOG.debug("Deleting ${messages.size} in ${violation.guild}")
                    val messageByChannel = mutableMapOf<String, MutableList<String>>()
                    messages.forEach { m ->
                        if (messageByChannel[m.channel] == null)
                            messageByChannel[m.channel] = mutableListOf()
                        messageByChannel[m.channel]?.add(m.id)
                    }
                    messageByChannel.forEach { chan, msgs ->
                        val channel = violation.guild.getTextChannelById(chan)
                        if (channel == null) {
                            Bot.LOG.debug("No channel found with $chan")
                            return@forEach
                        }
                        // Check for delete perms
                        if (!channel.checkPermissions(Permission.MESSAGE_MANAGE)) {
                            Bot.LOG.debug("No permissions in $channel")
                            return@forEach
                        }
                        val mutableMsgs = msgs.toMutableList()
                        while (mutableMsgs.isNotEmpty()) {
                            val list = mutableMsgs.subList(0, Math.min(100, mutableMsgs.size))
                            if (list.size == 1) {
                                channel.deleteMessageById(list.first()).queue()
                            } else {
                                channel.deleteMessagesByIds(list).queue()
                            }
                            mutableMsgs.removeAll(list)
                        }
                    }
                }
            } else {
                // Ignore
                Bot.LOG.debug(
                        "Last violation for ${violation.user} was < 10 seconds ago. Ignoring")
            }
        }
    }

    private fun getBucket(rule: String, guild: String, level: Int): Bucket? {
        val ruleJson = getRule(guild, level)?.optJSONObject(rule) ?: return null
        if (!ruleJson.has("count") || !ruleJson.has("period"))
            return null
        return Bucket("spam:$rule:$guild:%s", ruleJson.getInt("count"),
                ruleJson.getInt("period") * 1000)
    }

    private fun getSettings(guild: String): JSONObject {
        return SettingsRepository.getAsJsonObject(Bot.shardManager.getGuildById(guild)!!,
                "spam_settings", JSONObject(), true)!!
    }

    private fun getRule(guild: String, level: Int): JSONObject? {
        return getSettings(guild).optJSONObject(level.toString())
    }

    private fun getEffectiveRules(guild: Guild, user: User): List<Int> {
        val clearance = user.getClearance(guild)
        val settings = getSettings(guild.id)

        return settings.keySet().filter { it.isNumber() }.map { it.toInt() }.filter { it >= clearance }
    }

    private class ViolationException(val type: String, val msg: String, val user: User,
                                     val level: Int, val guild: Guild,
                                     val channel: MessageChannel) :
            Exception(msg) {
        override fun toString(): String {
            return "ViolationException(type='$type', msg='$msg', user=$user, level=$level, guild=$guild, channel=$channel)"
        }
    }
}