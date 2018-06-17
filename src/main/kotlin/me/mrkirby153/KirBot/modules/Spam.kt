package me.mrkirby153.KirBot.modules

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.QueryBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.database.models.guild.SpamSettings
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.utils.Bucket
import me.mrkirby153.KirBot.utils.EMOJI_RE
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.isNumber
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.logName
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import org.json.JSONObject
import java.sql.Timestamp
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

class Spam : Module("spam") {

    private val locks = mutableMapOf<String, Semaphore>()

    private val settingsCache: Cache<String, SpamSettings> = CacheBuilder.newBuilder().expireAfterWrite(
            1, TimeUnit.SECONDS).build()

    init {
        dependencies.add(Redis::class.java)
    }

    override fun onLoad() {

    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
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
                throw ViolationException("${check.toUpperCase()}: $friendlyText (${bucket.count(
                        message.author.id)}/${bucket.size(message.author.id)}s)", message.author,
                        level, message.guild)
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
    }

    private fun violate(violation: ViolationException) {
        ModuleManager[Redis::class.java].getConnection().use { con ->
            val key = "lv:${violation.guild.id}:${violation.user.id}"
            val last = (con.get(key) ?: "0").toLong()
            con.setex(key, 60, System.currentTimeMillis().toString())
            if (last + (10 * 1000) < System.currentTimeMillis()) {
                val settings = getSettings(violation.guild.id)
                val punishment = settings.optString("punishment") ?: return
                val duration = settings.optLong("punishment_duration", -1)

                // Modlog
                violation.guild.kirbotGuild.logManager.genericLog(LogEvent.SPAM_VIOLATE,
                        ":helmet_with_cross:",
                        "${violation.user.logName} Has violated ${violation.msg}")

                when (punishment.toUpperCase()) {
                    "NONE" -> {
                        // Do nothing
                    }
                    "MUTE" -> {
                        Infractions.mute(violation.user.id, violation.guild, violation.user.id,
                                "Spam detected")
                    }
                    "KICK" -> {
                        Infractions.kick(violation.user.id, violation.guild, violation.user.id,
                                "Spam detected")
                    }
                    "BAN" -> {
                        Infractions.ban(violation.user.id, violation.guild, violation.user.id,
                                "Spam detected")
                    }
                    "TEMPMUTE" -> {
                        Infractions.tempMute(violation.user.id, violation.guild, violation.user.id,
                                duration, TimeUnit.SECONDS, "Spam Detected")
                    }
                    else -> {
                        Bot.LOG.warn("Unknown punishment $punishment on guild ${violation.guild}")
                        violation.guild.kirbotGuild.logManager.genericLog(LogEvent.SPAM_VIOLATE,
                                ":warning:",
                                "Unknown punishment `${punishment.toUpperCase()}`. No action has been taken")
                    }
                }

                if (settings.has("clean_count") || settings.has("clean_duration")) {
                    Bot.LOG.debug("Performing clean")
                    Thread.sleep(250) // Wait to make sure in-flight stuff has been committed to the db
                    val messageQuery = Model.query(GuildMessage::class.java).where("author",
                            violation.user.id).where("server_id", violation.guild.id).where(
                            "deleted", false)
                    if (settings.has("clean_count")) {
                        val amount = (settings.optString("clean_count") ?: "0").toLong()
                        messageQuery.limit(amount)
                    }
                    if (settings.has("clean_duration")) {
                        val time = System.currentTimeMillis() - ((settings.optString(
                                "clean_duration") ?: "0").toLong() * 1000)
                        val after = Timestamp(time)
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
                        }
                        // Check for delete perms
                        if (!channel.checkPermissions(Permission.MESSAGE_MANAGE)) {
                            Bot.LOG.debug("No permissions in $channel")
                            return@forEach
                        }
                        val mutableMsgs = msgs.toMutableList()
                        while (mutableMsgs.isNotEmpty()) {
                            val list = mutableMsgs.subList(0, Math.min(100, mutableMsgs.size))
                            if(list.size == 1){
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
        val ruleJson = getRule(guild, level)?.getJSONObject(rule) ?: return null
        if (!ruleJson.has("count") || !ruleJson.has("period"))
            return null
        return Bucket("spam:$rule:$guild:%s", ruleJson.getInt("count"), ruleJson.getInt("period")*1000)
    }

    private fun getSettings(guild: String): JSONObject {
        val cached = settingsCache.getIfPresent(guild)
        if (cached != null) {
            return cached.settings
        }
        val model = Model.first(SpamSettings::class.java, "id", guild)
                ?: SpamSettings().apply { id = guild }
        settingsCache.put(guild, model)
        return model.settings
    }

    private fun getRule(guild: String, level: Int): JSONObject? {
        return getSettings(guild).optJSONObject(level.toString())
    }

    private fun getEffectiveRules(guild: Guild, user: User): List<Int> {
        val clearance = user.getClearance(guild)
        val settings = getSettings(guild.id)

        return settings.keySet().filter { it.isNumber() }.map { it.toInt() }.filter { it >= clearance }
    }

    private class ViolationException(val msg: String, val user: User, val level: Int,
                                     val guild: Guild) : Exception(msg) {
        override fun toString(): String {
            return "ViolationException(msg='$msg', user=$user, level=$level, guild=$guild)"
        }
    }
}