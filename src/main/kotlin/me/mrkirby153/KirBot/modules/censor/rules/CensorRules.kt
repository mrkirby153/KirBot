package me.mrkirby153.KirBot.modules.censor.rules

import me.mrkirby153.KirBot.utils.toTypedArray
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Invite
import net.dv8tion.jda.api.entities.Message
import org.json.JSONObject
import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Pattern

class ZalgoRule : CensorRule {
    private val zalgo = '\u0300'..'\u036F'
    override fun check(message: Message, config: JSONObject) {
        if (!config.optBoolean("zalgo", false))
            return
        message.contentRaw.forEachIndexed { index, char ->
            if (char in zalgo) {
                throw ViolationException("Found zalgo at position `$index`")
            }
        }
    }
}

class TokenRule : CensorRule {
    override fun check(message: Message, config: JSONObject) {
        val tokens = config.optJSONArray("blocked_tokens")?.toTypedArray(String::class.java)
                ?: arrayListOf()
        tokens.forEach {
            if (it.startsWith("r:")) {
                val r = Regex(it.substring(2))
                if (r.containsMatchIn(message.contentRaw.toLowerCase()))
                    throw ViolationException("Matched the regex token `${it.substring(2)}`")
            }
            if (message.contentRaw.toLowerCase().contains(it.toLowerCase()))
                throw ViolationException("Found blacklisted token `$it`")
        }
    }
}

class WordRule : CensorRule {
    override fun check(message: Message, config: JSONObject) {
        val words = config.optJSONArray("blocked_words")?.toTypedArray(String::class.java)
                ?: arrayListOf()
        words.forEach {
            var regex = false
            val pattern = if (it.startsWith("r:")) {
                // Regex
                regex = true
                it.substring(2)
            } else {
                Pattern.quote(it.toLowerCase())
            }
            val r = Regex("(^|\\s)$pattern(\\s|$)")
            if (r.containsMatchIn(message.contentRaw.toLowerCase())) {
                if (!regex)
                    throw ViolationException("Found blacklisted word `$it`")
                else
                    throw ViolationException("Matched the regex word `${it.substring(2)}`")
            }
        }
    }
}

class InviteRule : CensorRule {

    override fun check(message: Message, config: JSONObject) {
        val inviteSettings = config.optJSONObject("invites") ?: return
        val whitelistedInvites = inviteSettings.optJSONArray("whitelist")?.toTypedArray(
                String::class.java)
        val whitelistedGuilds = inviteSettings.optJSONArray("guild_whitelist")?.toTypedArray(
                String::class.java)

        val blacklistedInvites = inviteSettings.optJSONArray("blacklist")?.toTypedArray(
                String::class.java)
        val blacklistedGuilds = inviteSettings.optJSONArray("guild_blacklist")?.toTypedArray(
                String::class.java)

        if (!inviteSettings.optBoolean("enabled", false))
            return
        if (message.invites.isEmpty())
            return
        message.invites.forEach { invite ->
            val guild = resolve(message.jda, invite) ?: throw ViolationException("Invite `$invite`")
            if (guild.id == message.guild.id) // Allow invites to the current server
                return@forEach
            if (((blacklistedGuilds != null && blacklistedGuilds.isNotEmpty()) && guild.id in blacklistedGuilds))
                throw ViolationException("Invite `$invite` to ${guild.name}")
            if (((whitelistedGuilds != null && whitelistedGuilds.isNotEmpty()) && guild.id !in whitelistedGuilds))
                throw ViolationException("Invite `$invite` to ${guild.name}")
        }
    }

    fun resolve(jda: JDA, invite: String): Invite.Guild? {
        try {
            val inv = Invite.resolve(jda, invite).complete()
            return inv.guild
        } catch (e: Exception) {
            return null
        }
    }
}

class DomainRule : CensorRule {

    private val domainRegex = Regex(
            "([a-zA-Z0-9_]+\\.)*[a-zA-Z0-9][a-zA-Z0-9_-]+\\.[a-zA-z]{2,11}")

    private val urlRegex = Regex("(https?://[^\\s]+)")

    override fun check(message: Message, config: JSONObject) {
        val domainSettings = config.optJSONObject("domains") ?: return
        if (!domainSettings.optBoolean("enabled", false))
            return

        val whitelist = domainSettings.optJSONArray("whitelist")?.toTypedArray(String::class.java)
        val blacklist = domainSettings.optJSONArray("blacklist")?.toTypedArray(String::class.java)
        urlRegex.findAll(message.contentRaw).map { it.value }.forEach {
            try {
                val url = URL(it)
                val domain = url.host
                if ((whitelist != null && whitelist.isNotEmpty()) && domain !in whitelist) {
                    throw ViolationException("Not whitelisted domain: `$domain`")
                } else if ((blacklist != null && blacklist.isNotEmpty()) && domain in blacklist) {
                    throw ViolationException("Blacklisted domain: `$domain`")
                }
            } catch (ignored: MalformedURLException) {
                // Ignore invalid URLs
            }
        }
    }
}

