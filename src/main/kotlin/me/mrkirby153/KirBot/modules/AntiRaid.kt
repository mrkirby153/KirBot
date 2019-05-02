package me.mrkirby153.KirBot.modules

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.utils.SettingsRepository
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.KirBot.utils.toTypedArray
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

class AntiRaid : Module("AntiRaid") {

    private val guildBucketCache = CacheBuilder.newBuilder().maximumSize(100).build(
            object : CacheLoader<String, LeakyBucket>() {
                override fun load(key: String): LeakyBucket {
                    val g = Bot.shardManager.getGuild(key)!!
                    return LeakyBucket("antiraid:join:$key",
                            SettingsRepository.get(g, "anti_raid_count", "0")!!.toInt(),
                            SettingsRepository.get(g, "anti_raid_period", "0")!!.toInt() * 1000)
                }
            }
    )

    val activeRaids = mutableMapOf<String, Raid>()
    private val statusMessages = mutableMapOf<Raid, Message>()

    override fun onLoad() {
        val listener: (Guild, String?) -> Unit = { guild, _ ->
            Bot.LOG.debug("Raid settings changed on $guild. Clearing cached bucket data")
            guildBucketCache.invalidate(guild.id)
        }
        SettingsRepository.registerSettingListener("anti_raid_count", listener)
        SettingsRepository.registerSettingListener("anti_raid_period", listener)
    }

    @Subscribe
    fun onJoin(event: GuildMemberJoinEvent) {
        if (SettingsRepository.get(event.guild, "anti_raid_enabled", "0") == "1")
            handleJoin(event.member)
    }

    fun raidActive(guild: Guild): Boolean {
        return this.activeRaids.values.map { it.guild }.contains(guild.id)
    }

    fun dismissActiveRaid(guild: Guild) {
        val raid = this.activeRaids[guild.id] ?: return
        raid.active = false
        raid.members.forEach {
            Infractions.unmute(it, guild, guild.selfMember.user.id)
        }
    }

    fun punishAllRaiders(guild: Guild, id: String, action: String, msg: String? = null) {
        getRaid(guild, id)?.members?.mapNotNull {
            guild.getMemberById(it.id)
        }?.forEach { member ->
            punishRaider(member, action, msg)
        }
    }

    fun getRaid(guild: Guild, id: String): RaidInfo? {
        ModuleManager[Redis::class].getConnection().use {
            val str = it.get("raid:${guild.id}:$id") ?: return null
            val json = JSONObject(JSONTokener(str))
            val users = json.getJSONArray("members").toTypedArray(
                    JSONObject::class.java).map { raidJson ->
                RaidMember(raidJson.optString("id", "0"), raidJson.optString("name", null))
            }
            return RaidInfo(json.optString("id", "UNKNOWN"), json.optString("timestamp", "???"),
                    users)
        }
    }

    private fun handleJoin(member: Member) {
        debug("Processing join for $member")
        val joinBucket = this.guildBucketCache[member.guild.id]
        val action = SettingsRepository.get(member.guild, "anti_raid_action", "NONE")!!
        if (raidActive(member.guild)) {
            // Record the join
            val raid = this.activeRaids[member.guild.id] ?: return
            raid.lastJoin = System.currentTimeMillis()
            if (!raid.active)
                return
            raid.members.add(member.user.id)
            punishRaider(member, action, "Member of raid ${raid.id}")
            return
        }
        if (joinBucket.insert(member.user.id)) {
            // Raid alarm tripped
            val raid = getNewRaid(member.guild)
            alert(raid, joinBucket.count(), joinBucket.time())
            joinBucket.get().mapNotNull {
                member.guild.getMemberById(it)
            }.forEach { m ->
                punishRaider(m, action, "Member of raid ${raid.id}")
                raid.members.add(m.user.id)
            }
            joinBucket.empty()
        }
    }

    private fun punishRaider(member: Member, action: String, msg: String? = null) {
        when (action.toLowerCase()) {
            "nothing" -> {
                // Do nothing
            }
            "kick" -> {
                Infractions.kick(member.user.id, member.guild, member.guild.selfMember.user.id, msg)
            }
            "mute" -> {
                Infractions.mute(member.user.id, member.guild, member.guild.selfMember.user.id, msg,
                        false)
            }
            "ban" -> {
                Infractions.ban(member.user.id, member.guild, member.guild.selfMember.user.id, msg)
            }
        }
    }

    fun unmuteAllRaiders(guild: Guild, raid: String) {
        getRaid(guild, raid)?.members?.mapNotNull { guild.getMemberById(it.id) }?.forEach {
            Infractions.removeMutedRole(it.user, guild)
        }
    }

    private fun getNewRaid(guild: Guild): Raid {
        val raid = Raid(Bot.idGenerator.generate().toString(), guild.id, System.currentTimeMillis())
        this.activeRaids[guild.id] = raid
        return raid
    }

    private fun getResetTime(guild: Guild): Long {
        val raid = this.activeRaids[guild.id] ?: return 0
        val lastJoin = raid.lastJoin
        return (lastJoin + SettingsRepository.get(guild, "anti_raid_quiet_period",
                "0")!!.toInt() * 1000) - System.currentTimeMillis()
    }

    private fun alert(raid: Raid, amount: Int, time: Double) {
        Bot.LOG.debug("RAID DETECTED: $raid")
        val guild = Bot.shardManager.getGuild(raid.guild) ?: return
        val alertRole = SettingsRepository.get(guild, "anti_raid_alert_role", "0")
        val toPing: Role? = if (alertRole != null && (alertRole == "@everyone" || alertRole == "@here")) null else guild.getRoleById(
                alertRole)

        val prefix = SettingsRepository.get(guild, "command_prefix", "!")
        val msg = buildString {
            if (toPing != null) {
                append(toPing.asMention)
                append(" ")
            } else {
                if (alertRole == "@everyone" || alertRole == "@here") {
                    append(alertRole)
                    append(" ")
                }
            }
            append("A raid has been detected - $amount joins in $time seconds (ID: ${raid.id})\n")
            append("Type `${prefix}raid dismiss` to dismiss this raid as a false alarm")
        }
        val mentionable = toPing?.isMentionable
        if (mentionable == false) {
            toPing.manager.setMentionable(true).complete()
        }
        val channel = getAlertChannel(guild) ?: return
        channel.sendMessage(msg).queue {
            toPing?.manager?.setMentionable(mentionable!!)?.queue()
        }
    }

    private fun updateStatusMessage(raid: Raid) {
        val guild = Bot.shardManager.getGuild(raid.guild) ?: return
        fun buildStatusMessage(): String {
            return "Total Raiders: ${raid.members.size}\nTime until reset: ${Time.format(1,
                    this.getResetTime(
                            guild))}"
        }

        val msg = this.statusMessages[raid]
        if (msg == null) {
            val channel = getAlertChannel(guild) ?: return
            val m = channel.sendMessage(buildStatusMessage()).complete()
            this.statusMessages[raid] = m
        } else {
            msg.editMessage(buildStatusMessage()).queue()
        }
    }

    private fun getAlertChannel(guild: Guild): TextChannel? {
        val get = SettingsRepository.get(guild, "anti_raid_alert_channel", "0")
        return guild.getTextChannelById(get)
    }

    private fun recordRaidMembers(raid: Raid) {
        val key = "raid:${raid.guild}:${raid.id}"
        ModuleManager[Redis::class.java].getConnection().use {
            val json = JSONObject()
            json.put("id", raid.id)
            json.put("timestamp", Time.now())
            json.put("member_count", raid.members.size)
            val memberArray = JSONArray()
            val guild = Bot.shardManager.getGuild(raid.guild)
            raid.members.forEach { m ->
                val memberJson = JSONObject()
                memberJson.put("id", m)
                if (guild != null) {
                    memberJson.put("name", guild.jda.getUserById(m)?.nameAndDiscrim)
                }
                memberArray.put(memberJson)
            }
            json.put("members", memberArray)
            it.set(key, json.toString())
            it.expire(key, 60 * 60 * 24 * 30)
        }
    }

    @Periodic(5)
    private fun updateRaidMessages() {
        this.activeRaids.values.forEach { r ->
            this.updateStatusMessage(r)
        }
    }

    @Periodic(1)
    private fun dismissExpiredRaids() {
        val toRemove = mutableListOf<String>()
        this.activeRaids.keys.mapNotNull { Bot.shardManager.getGuild(it) }.forEach { guild ->
            if (getResetTime(guild) <= 0) {
                // Dismiss the raid
                val raid = activeRaids[guild.id]!!
                if (raid.active) {
                    val remainingMembers = raid.members.mapNotNull { guild.getMemberById(it) }.size
                    recordRaidMembers(raid)
                    val punishment = SettingsRepository.get(guild, "anti_raid_action", "NONE")
                    val msg = buildString {
                        appendln("Raid `${raid.id}` has concluded")
                        appendln("Total members: ${raid.members.size}")
                        val prefix = SettingsRepository.get(guild, "command_prefix", "!")
                        if (punishment == "MUTE") {
                            appendln(
                                    "$remainingMembers members have been locked in for further moderation action.")
                            appendln()
                            appendln("Type `${prefix}raid ban ${raid.id}` to ban the raiders")
                            appendln("Type `${prefix}raid kick ${raid.id}` to kick the raiders")
                            appendln("Type `${prefix}raid unmute ${raid.id}` to unmute the raiders")
                        }
                        appendln(
                                "Type `${prefix}raid users ${raid.id}` for information about the raid")
                    }
                    getAlertChannel(guild)?.sendMessage(msg)?.queue()
                } else {
                    getAlertChannel(guild)?.sendMessage(
                            "Previously dismissed raid has expired. Raid detection reactivated")?.queue()
                }
                toRemove.add(guild.id)
            }
        }
        this.activeRaids.entries.removeIf { it.key in toRemove }
    }

    private class LeakyBucket(val key: String, val maxActions: Int, val timePeriod: Int) {

        fun insert(value: String): Boolean {
            this.clearExpired()
            ModuleManager[Redis::class.java].getConnection().use { con ->
                con.zadd(this.key, System.currentTimeMillis().toDouble(), value)
                con.expire(this.key, this.timePeriod / 1000)
                val c = con.zcount(this.key, "-inf", "inf")
                Bot.LOG.debug("[LEAKY] $key $c / $maxActions")
                return c >= maxActions
            }
        }

        fun clearExpired() {
            ModuleManager[Redis::class.java].getConnection().use {
                it.zremrangeByScore(this.key, "-inf",
                        (System.currentTimeMillis() - timePeriod).toString())
            }
        }

        fun get(): MutableSet<String> {
            this.clearExpired()
            ModuleManager[Redis::class.java].getConnection().use {
                return it.zrangeByScore(this.key, "-inf", "inf")
            }
        }

        fun empty() {
            ModuleManager[Redis::class.java].getConnection().use {
                it.zremrangeByScore(this.key, "-inf", "inf")
            }
        }

        fun count(): Int {
            ModuleManager[Redis::class.java].getConnection().use { con ->
                return con.zcount(this.key, "-inf", "inf").toInt()
            }
        }

        fun time(): Double {
            ModuleManager[Redis::class.java].getConnection().use { con ->
                val d = con.zrangeByScore(this.key, "-inf", "inf")
                if (d.size <= 1)
                    return 0.0
                return (con.zscore(this.key, d.last()) - con.zscore(this.key,
                        d.first())) / 1000.0
            }
        }
    }

    class Raid(val id: String, val guild: String, val time: Long,
               var active: Boolean = true,
               var lastJoin: Long = System.currentTimeMillis(),
               val members: MutableSet<String> = mutableSetOf()) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Raid) return false
            if (id != other.id) return false
            return true
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }
    }

    class RaidInfo(val id: String, val timestamp: String, val members: List<RaidMember>)
    class RaidMember(val id: String, val name: String?)
}