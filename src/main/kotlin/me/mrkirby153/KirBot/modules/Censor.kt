package me.mrkirby153.KirBot.modules

import com.google.common.cache.CacheBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.guild.CensorSettings
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Invite
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import org.json.JSONObject
import java.util.Arrays
import java.util.concurrent.TimeUnit

class Censor : Module("censor") {


    private val zalgo = '\u0300'..'\u036F'

    private val invitePattern = "(discord\\.gg|discordapp.com/invite)/([A-Za-z0-9\\-]+)"
    private val inviteRegex = Regex(invitePattern)

    private val cache = CacheBuilder.newBuilder().expireAfterWrite(2,
            TimeUnit.SECONDS).build<String, CensorSettings>()


    override fun onLoad() {

    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author == event.jda.selfUser)
            return
        process(event.message)
    }

    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        if (event.author == event.jda.selfUser)
            return
        process(event.message)
    }

    private fun process(message: Message) {
        val matches = matchesRules(message)
        Bot.LOG.debug("Found ${matches.size} violations of the censor")
        if (matches.isNotEmpty()) {
            // Delete and log in the modlogs
            val firstMatch = matches.entries.first()
            val matches = if (firstMatch.value.isNotEmpty()) "`${firstMatch.value.joinToString(
                    ", ")}`" else ""
            message.guild.kirbotGuild.logManager.genericLog(":no_entry_sign:",
                    "Message by **${message.author.nameAndDiscrim}** (`${message.author.id}`) censored in #${message.channel.name}: ${firstMatch.key.friendlyName} $matches ```${message.contentRaw}```")
            message.delete().queue()
        }
    }

    private fun matchesRules(message: Message): Map<CensorResult, Array<String>> {
        val settings = getEffectiveSettings(message.author, message.guild)

        val matchedResults = mutableMapOf<CensorResult, Array<String>>()

        settings.forEach { s ->
            if (s.zalgo) {
                if (containsZalgo(message.contentRaw))
                    matchedResults[CensorResult.ZALGO] = emptyArray()
            }

            val matchedTokens = mutableListOf<String>()
            s.blockedTokens.forEach {
                if (message.contentRaw.toLowerCase().contains(it)) {
                    matchedTokens.add(it)
                }
            }
            if (matchedTokens.isNotEmpty()) {
                matchedResults[CensorResult.BLOCKED_TOKEN] = matchedTokens.toTypedArray()
            }

            val matchedWords = mutableListOf<String>()
            s.blockedWords.forEach { w ->
                val r = Regex("\\s$w\\s")
                if (r.containsMatchIn(message.contentRaw.toLowerCase()))
                    matchedWords.add(w)
            }
            if (matchedWords.isNotEmpty())
                matchedResults[CensorResult.BLOCKED_WORD] = matchedWords.toTypedArray()

            // Invites
            val matchedInvites = mutableListOf<String>()
            if (s.inviteSettings.enabled && message.invites.isNotEmpty()) {
                val whitelist = s.inviteSettings.blacklist.isEmpty()
                val inviteCodes = message.invites

                for (i in 0 until inviteCodes.size) {
                    val invite = inviteCodes[i]

                    if (whitelist) {
                        if (invite in s.inviteSettings.whitelist)
                            break
                        try {
                            val resolved = Invite.resolve(message.jda, invite).complete()
                            if (resolved.guild.id in s.inviteSettings.guildWhitelist || resolved.guild.id == message.guild.id)
                                break
                        } catch (e: Exception) {
                            // Ignore
                        }
                        matchedInvites.add(invite)
                    } else {
                        if (invite in s.inviteSettings.blacklist)
                            matchedInvites.add(invite)
                    }
                }
            }

            if (matchedInvites.isNotEmpty())
                matchedResults[CensorResult.BLACKLIST_INVITE] = matchedInvites.toTypedArray()

            val matchedDomains = mutableListOf<String>()
            // Domains
            if (s.domainSettings.enabled) {
                val whitelist = s.domainSettings.blacklist.isEmpty()
                if (whitelist) {
                    // TODO 4/4/18: Domain whitelisting
                } else {
                    s.domainSettings.blacklist.forEach { blacklist ->
                        if (message.contentRaw.contains(blacklist))
                            matchedDomains.add(blacklist)
                    }
                }
            }
            if (matchedDomains.isNotEmpty())
                matchedResults[CensorResult.BLACKLIST_WEBSITE] = matchedDomains.toTypedArray()
        }

        return matchedResults.toMap()
    }


    fun containsZalgo(text: String): Boolean {
        return text.filter { it in zalgo }.count() > 0
    }

    private fun getSettings(guild: Guild): CensorSettings {
        val settings = cache.getIfPresent(guild.id)
        return if (settings == null) {
            val s = Model.Companion.first(CensorSettings::class.java, guild.id)
                    ?: CensorSettings().apply { this.id = guild.id }
            s
        } else {
            settings
        }
    }

    private fun getEffectiveSettings(user: User, guild: Guild): Array<CensorRule> {
        val clearance = user.getClearance(guild)

        val settings = getSettings(guild).settings

        val effectiveCats = settings.keySet().map { it.toInt() }.filter { it >= clearance }

        val rules = mutableListOf<CensorRule>()

        effectiveCats.forEach { cat ->
            rules.add(decodeRule(settings.getJSONObject(cat.toString())))
        }
        return rules.toTypedArray()
    }


    private fun decodeRule(obj: JSONObject): CensorRule {
        val inviteSettings = obj.optJSONObject("invites")
        var parsedInvite: InviteSettings? = null
        if (inviteSettings != null) {
            parsedInvite = InviteSettings(inviteSettings.optBoolean("enabled", false),
                    inviteSettings.optJSONArray("whitelist")?.map { it.toString() }?.toTypedArray()
                            ?: emptyArray(),
                    inviteSettings.optJSONArray("blacklist")?.map { it.toString() }?.toTypedArray()
                            ?: emptyArray(), inviteSettings.optJSONArray(
                    "guild_whitelist")?.map { it.toString() }?.toTypedArray() ?: emptyArray())
        }

        var parsedDomains: DomainSettings? = null
        val domainSettings = obj.optJSONObject("domains")
        if (domainSettings != null) {
            parsedDomains = DomainSettings(domainSettings.optBoolean("enabled", false),
                    domainSettings.optJSONArray("whitelist")?.map { it.toString() }?.toTypedArray()
                            ?: emptyArray(),
                    domainSettings.optJSONArray("blacklist")?.map { it.toString() }?.toTypedArray()
                            ?: emptyArray())
        }

        return CensorRule(obj.optBoolean("zalgo", false),
                obj.optJSONArray("blocked_words")?.map { it.toString() }?.toTypedArray()
                        ?: emptyArray(),
                obj.optJSONArray("blocked_tokens")?.map { it.toString() }?.toTypedArray()
                        ?: emptyArray(),
                parsedInvite ?: InviteSettings(false, emptyArray(), emptyArray(), emptyArray()),
                parsedDomains ?: DomainSettings(false, emptyArray(), emptyArray()))
    }

    private data class CensorRule(val zalgo: Boolean, val blockedWords: Array<String>,
                                  val blockedTokens: Array<String>,
                                  val inviteSettings: InviteSettings,
                                  val domainSettings: DomainSettings) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CensorRule

            if (zalgo != other.zalgo) return false
            if (!Arrays.equals(blockedWords, other.blockedWords)) return false
            if (!Arrays.equals(blockedTokens, other.blockedTokens)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = zalgo.hashCode()
            result = 31 * result + Arrays.hashCode(blockedWords)
            result = 31 * result + Arrays.hashCode(blockedTokens)
            return result
        }
    }

    private data class InviteSettings(val enabled: Boolean, val whitelist: Array<String>,
                                      val blacklist: Array<String>,
                                      val guildWhitelist: Array<String>) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as InviteSettings

            if (enabled != other.enabled) return false
            if (!Arrays.equals(whitelist, other.whitelist)) return false
            if (!Arrays.equals(blacklist, other.blacklist)) return false
            if (!Arrays.equals(guildWhitelist, other.guildWhitelist)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = enabled.hashCode()
            result = 31 * result + Arrays.hashCode(whitelist)
            result = 31 * result + Arrays.hashCode(blacklist)
            result = 31 * result + Arrays.hashCode(guildWhitelist)
            return result
        }
    }

    private data class DomainSettings(val enabled: Boolean, val whitelist: Array<String>,
                                      val blacklist: Array<String>) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as DomainSettings

            if (enabled != other.enabled) return false
            if (!Arrays.equals(whitelist, other.whitelist)) return false
            if (!Arrays.equals(blacklist, other.blacklist)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = enabled.hashCode()
            result = 31 * result + Arrays.hashCode(whitelist)
            result = 31 * result + Arrays.hashCode(blacklist)
            return result
        }
    }

    private enum class CensorResult(val friendlyName: String) {
        ZALGO("Zalgo"),
        BLACKLIST_INVITE("Blacklisted Invite"),
        BLACKLIST_WEBSITE("Blacklisted Website"),
        BLOCKED_TOKEN("Blocked Token"),
        BLOCKED_WORD("Blocked word")
    }
}