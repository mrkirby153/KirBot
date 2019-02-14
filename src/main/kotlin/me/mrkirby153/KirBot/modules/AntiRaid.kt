package me.mrkirby153.KirBot.modules

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.listener.WaitUtils
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.utils.RED_CROSS
import me.mrkirby153.KirBot.utils.RED_TICK
import me.mrkirby153.KirBot.utils.SettingsRepository
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.kcutils.Time
import me.mrkirby153.kcutils.utils.SnowflakeWorker
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import java.util.concurrent.ConcurrentHashMap

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

    private val keyGen = SnowflakeWorker(1, 1)

    private val activeRaids = mutableMapOf<String, RaidInfo>()

    private val dismissedGuilds = mutableListOf<String>()

    private val lastJoin = ConcurrentHashMap<String, Long>()

    private val statusMessages = ConcurrentHashMap<Guild, Message>()

    private val DOOR_EMOJI = "\uD83D\uDEAA"
    private val BOOT_EMOJI = "\uD83D\uDC62"

    override fun onLoad() {
        val listener: (Guild, String?) -> Unit = { guild, _ ->
            Bot.LOG.debug("raid settings changed on ${guild.name}, evicting cache")
            guildBucketCache.invalidate(guild.id)
        }
        SettingsRepository.registerSettingListener("anti_raid_count", listener)
        SettingsRepository.registerSettingListener("anti_raid_period", listener)
    }

    @Subscribe
    fun onJoin(event: GuildMemberJoinEvent) {
        if (SettingsRepository.get(event.guild, "anti_raid_enabled", "0") == "1")
            handleJoin(event.guild, event.user)
    }

    fun handleJoin(guild: Guild, user: User) {
        debug("Processing join for $user on $guild")
//        val raidSettings = this.raidSettingsCache[guild.id]
        val joinBucket = this.guildBucketCache[guild.id]
        val action = SettingsRepository.get(guild, "anti_raid_action", "NONE")!!
        if (guild.id in this.dismissedGuilds)
            return // The raid has been dismissed, we don't want to do anything
        if (activeRaids.containsKey(guild.id)) {
            // A raid is in process, perform the action
            recordRaidMember(guild, user.id)
            punishRaider(guild, user, action)
            scheduleTermination(guild)
        } else {
            if (joinBucket.insert(user.id)) {
                debug("Raid threshold has been tripped on ${guild.id}")
                // Raid alarm has been tripped
                this.activeRaids[guild.id] = RaidInfo(keyGen.generate().toString(),
                        System.currentTimeMillis())
                alertStaff(guild)
                scheduleTermination(guild)

                // Handle existing users that have joined within the period
                joinBucket.get().map { guild.getMemberById(it).user }.forEach {
                    debug("Taking action on $it")
                    recordRaidMember(guild, it.id)
                    punishRaider(guild, it, action)
                }
            }
        }
    }

    private fun recordRaidMember(guild: Guild, user: String) {
        val raidId = this.activeRaids[guild.id]?.id ?: return
        ModuleManager[Redis::class.java].getConnection().use {
            it.sadd("antiraid:raid:$raidId", user)
            // Expire the key in 30 days
            it.expire("antiraid:raid:$raidId", 60 * 60 * 24 * 30)
        }
    }

    private fun getRaidMembers(id: String): Set<String> {
        ModuleManager[Redis::class.java].getConnection().use {
            return it.smembers("antiraid:raid:$id")
        }
    }

    @Periodic(1)
    fun terminateRaids() {
        val terminated = mutableListOf<String>()
        this.lastJoin.forEach {
            if (it.value + SettingsRepository.get(Bot.shardManager.getGuild(it.key)!!,
                            "anti_raid_quiet_period",
                            "0")!!.toInt() * 1000 < System.currentTimeMillis()) {
                val guild = Bot.shardManager.getGuild(it.key) ?: return@forEach
                terminateRaid(guild)
                terminated.add(it.key)
            }
        }
        terminated.forEach {
            this.lastJoin.remove(it)
        }
    }

    @Periodic(5)
    fun updateStatusMessages() {
        this.statusMessages.forEach {
            updateStatusMessage(it.key)
        }
    }

    private fun scheduleTermination(guild: Guild) {
        this.lastJoin[guild.id] = System.currentTimeMillis()
    }

    fun punishRaider(guild: Guild, user: User, action: String) {
        val infMessage = "Member of raid on ${Time.now()}"
        when (action.toLowerCase()) {
            "nothing" -> {
                // Do nothing, maybe the
            }
            "kick" -> {
                Infractions.kick(user.id, guild, guild.selfMember.user.id, infMessage)
            }
            "mute" -> {
                Infractions.mute(user.id, guild, guild.selfMember.user.id, infMessage, false)
            }
            "ban" -> {
                Infractions.ban(user.id, guild, guild.selfMember.user.id, infMessage)
            }
            else -> {
                // Do nothing? Maybe we should throw an error
            }
        }
    }

    fun alertStaff(guild: Guild) {
        val alertRole = SettingsRepository.get(guild, "anti_raid_alert_role")
        val alertChannel = getAlertChannel(guild) ?: return
        val roleToPing: Role? = if (alertRole != null && (alertRole == "@everyone" || alertRole == "@here")) null else guild.getRoleById(
                alertRole)
        val msg = buildString {
            if (roleToPing != null) {
                append(roleToPing.asMention)
            } else {
                if (alertRole == "@everyone" || alertRole == "@here") {
                    append(alertRole)
                }
            }
            append(" ")
            append("A raid has been detected (${this@AntiRaid.activeRaids[guild.id]?.id})\n\n$RED_TICK - Dismiss raid as false alarm")
        }
        var prev = false
        if (roleToPing != null) {
            prev = roleToPing.isMentionable
            roleToPing.manager.setMentionable(true).complete()
        }
        val m = alertChannel.sendMessage(msg).complete()
        m.addReaction(RED_TICK.emote).complete()
        WaitUtils.waitFor(MessageReactionAddEvent::class.java) { evt ->
            if (evt.member == evt.guild.selfMember || evt.messageId != m.id)
                return@waitFor
            if (evt.reactionEmote.emote == RED_TICK.emote) {
                dismissRaid(guild)
                scheduleTermination(guild)
                cancel()
            }
        }
        roleToPing?.manager?.setMentionable(prev)?.queue()
        updateStatusMessage(guild)
    }

    private fun getAlertChannel(guild: Guild): TextChannel? {
        return guild.getTextChannelById(
                SettingsRepository.get(guild, "anti_raid_alert_channel", "0"))
    }

    private fun terminateRaid(guild: Guild) {
        // Raid has concluded, give the admins options on what to do
        debug("Terminating raid on $guild")
        statusMessages.remove(guild)
        val raidInfo = this.activeRaids.remove(guild.id) ?: return
        this.guildBucketCache[guild.id].empty()
        if (this.dismissedGuilds.remove(guild.id)) {
            getAlertChannel(guild)?.sendMessage(":warning: Ready for next raid")?.queue()
            debug("Guild has been dismissed, no further action is needed")
            return
        }
        val chan = getAlertChannel(guild) ?: return
        val raid = raidInfo.id
        val members = this.getRaidMembers(raid)
        val currentMembers = members.filter { guild.getMemberById(it) != null }
        chan.sendMessage(embed {
            title {
                +"Raid $raid has concluded"
            }
            description {
                +"**${members.size}** users joined\n"
                +"**${members.size - currentMembers.size}** users are no longer in the server\n"
                +"Total Duration: ${Time.format(1,
                        System.currentTimeMillis() - raidInfo.startTime)}"
                if (SettingsRepository.get(guild, "anti_raid_action", "NONE") == "MUTE") {
                    +"\n\n$DOOR_EMOJI - Ban all raiders"
                    +"\n\n$BOOT_EMOJI - Kick all raiders"
                    +"\n\n$RED_CROSS - Dismiss raid as false alarm and unmute"
                }
            }
        }.build()).queue { msg ->
            if (SettingsRepository.get(guild, "anti_raid_action", "NONE") == "MUTE") {
                msg.addReaction(DOOR_EMOJI).queue()
                msg.addReaction(BOOT_EMOJI).queue()
                msg.addReaction(RED_CROSS).queue()
                WaitUtils.waitFor(MessageReactionAddEvent::class.java) {
                    if (it.messageId != msg.id || it.member == guild.selfMember)
                        return@waitFor
                    when (it.reactionEmote.name) {
                        BOOT_EMOJI -> {
                            msg.channel.sendMessage("Kicking all raiders").queue()
                            members.mapNotNull { guild.getMemberById(it) }.forEach {
                                punishRaider(guild, it.user, "kick")
                            }
                            cancel()
                        }
                        DOOR_EMOJI -> {
                            msg.channel.sendMessage("Banning all raiders").queue()
                            members.mapNotNull { guild.getMemberById(it) }.forEach {
                                punishRaider(guild, it.user, "ban")
                            }
                            cancel()
                        }
                        RED_CROSS -> {
                            msg.channel.sendMessage("Unmuting all raiders").queue()
                            members.mapNotNull { guild.getMemberById(it) }.forEach {
                                Infractions.unmute(it.user.id, guild, guild.selfMember.user.id,
                                        "Raid dismissed")
                            }
                            cancel()
                        }
                        else -> {
                            // Do nothing
                        }
                    }
                }
            }
        }
    }

    fun dismissRaid(guild: Guild) {
        // Unmute everyone if they've been muted
        this.dismissedGuilds.add(guild.id)
        getAlertChannel(guild)?.sendMessage(buildString {
            append("Raid dismissed as a false alarm.")
            if (SettingsRepository.get(guild, "anti_raid_action", "NONE") == "MUTE") {
                append(" Unmuting all members")
            }
        })?.queue()
        getRaidMembers(this.activeRaids[guild.id]!!.id).mapNotNull {
            guild.getMemberById(it)
        }.forEach {
            Infractions.unmute(it.user.id, guild, guild.selfMember.user.id, "Raid dismissed")
        }
    }

    fun updateStatusMessage(guild: Guild) {
//        val settings = raidSettingsCache[guild.id]
        val timeLeft = ((this.lastJoin[guild.id] ?: System.currentTimeMillis())
                + (SettingsRepository.get(guild, "anti_raid_quiet_period",
                "0")!!.toInt() * 1000)) - System.currentTimeMillis()
        val members = getRaidMembers(activeRaids[guild.id]!!.id)

        val msg = this.statusMessages[guild]
        val statusMsg = "Total Raiders: ${members.size}\nTime until reset: ${Time.formatLong(
                timeLeft)}"
        if (msg == null) {
            getAlertChannel(guild)?.sendMessage(
                    statusMsg)?.queue {
                this.statusMessages[guild] = it
            }
        } else {
            msg.editMessage(statusMsg).queue()
        }
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
    }

    private data class RaidInfo(val id: String, val startTime: Long)

}