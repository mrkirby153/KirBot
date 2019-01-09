package me.mrkirby153.KirBot.modules.censor.rules

import me.mrkirby153.KirBot.utils.toTypedArray
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.entities.Invite
import net.dv8tion.jda.core.entities.Message
import org.json.JSONObject
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
            val r = Regex("(^|\\s)${Pattern.quote(it.toLowerCase())}(\\s|$)")
            if (r.containsMatchIn(message.contentRaw.toLowerCase()))
                throw ViolationException("Found blacklisted word `$it`")
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
            if(guild.id == message.guild.id) // Allow invites to the current server
                return@forEach
            if ((blacklistedGuilds != null && guild.id in blacklistedGuilds) || (blacklistedInvites != null && invite in blacklistedInvites))
                throw ViolationException("Invite `$invite` to ${guild.name}")
            if ((whitelistedGuilds != null && guild.id !in whitelistedGuilds) || (whitelistedInvites != null && invite !in whitelistedInvites))
                throw ViolationException("Invite `$invite` to ${guild.name}")
        }
    }

    fun resolve(jda: JDA, invite: String): Invite.Guild? {
        try {
            val inv = Invite.resolve(jda, invite).complete()
            return inv.guild
        } catch(e: Exception){
            return null
        }
    }
}

class DomainRule : CensorRule {

    private val domainRegex = Regex(
            "([a-zA-Z0-9_]+\\.)*[a-zA-Z0-9][a-zA-Z0-9_-]+\\.[a-zA-z]{2,11}")

    override fun check(message: Message, config: JSONObject) {
        val domainSettings = config.optJSONObject("domains") ?: return
        if (!domainSettings.optBoolean("enabled", false))
            return
        val whitelist = domainSettings.optJSONArray("whitelist")?.toTypedArray(String::class.java)
        val blacklist = domainSettings.optJSONArray("blacklist")?.toTypedArray(String::class.java)

        val domains = domainRegex.findAll(message.contentRaw).map { it.value }
        domains.forEach { domain ->
            if ((whitelist != null && domain !in whitelist) || (blacklist != null && domain in blacklist))
                throw ViolationException("Blacklisted domain `$domain`")
        }
    }
}

