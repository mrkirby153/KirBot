package com.mrkirby153.kirbot.services.censor

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Invite
import net.dv8tion.jda.api.entities.Message
import java.net.MalformedURLException
import java.net.URL
import java.util.regex.Pattern

/**
 * Generic interface for all censor rules
 */
interface CensorRule {

    /**
     * Checks if the provided [message] violates the provided [settings].
     * @throws ViolationException If the provided message violates the censor settings
     */
    fun check(message: Message, settings: CensorSetting)
}

/**
 * Rule for checking for zalgo
 */
class ZalgoRule : CensorRule {

    private val zalgoRange = '\u0300'..'\u036F'

    override fun check(message: Message, settings: CensorSetting) {
        if (!settings.zalgo)
            return
        message.contentRaw.forEachIndexed { index, c ->
            if (c in zalgoRange)
                throw ViolationException("Found zalgo at position `$index`")
        }
    }
}

/**
 * Rule for matching tokens. Tokens can appear at any point in the text
 */
class TokenRule : CensorRule {
    override fun check(message: Message, settings: CensorSetting) {
        val tokens = settings.blockedTokens
        tokens.forEach { token ->
            if (token.startsWith("r:")) {
                val regex = Regex(token.substring(2))
                if (regex.containsMatchIn(message.contentRaw.toLowerCase()))
                    throw ViolationException(
                            "Matched the regular expression `${token.substring(2)}`")
            } else {
                if (message.contentRaw.toLowerCase().contains(token.toLowerCase()))
                    throw ViolationException("Found blacklisted token `$token`")
            }
        }
    }
}

/**
 * Rule for matching words. Words **must** be bounded on either side by a space or the start/end of
 * the string
 */
class WordRule : CensorRule {
    override fun check(message: Message, settings: CensorSetting) {
        val words = settings.blockedWords
        words.forEach { word ->
            var isRegex = false
            val pattern = if (word.startsWith("r:")) {
                isRegex = true
                word.substring(2)
            } else {
                Pattern.quote(word.toLowerCase())
            }
            val regex = Regex("(^|\\s)$pattern(\\s|$)") // Assert word bounded
            if (regex.containsMatchIn(message.contentRaw.toLowerCase())) {
                if (isRegex)
                    throw ViolationException(
                            "Matched the regular expression word `${word.substring(2)}`")
                else
                    throw ViolationException("Found blacklisted word `$word`")
            }
        }
    }
}

/**
 * Rule for matching server invites
 */
class InviteRule : CensorRule {
    override fun check(message: Message, settings: CensorSetting) {
        if (!settings.invites.enabled || message.invites.isEmpty())
            return
        val whitelistedGuilds = settings.invites.whitelist
        val blacklistedGuilds = settings.invites.blacklist

        message.invites.forEach { invite ->
            val targetGuild = resolve(message.jda, invite) ?: throw ViolationException("Invite `$invite`")

            if (targetGuild.id == message.guild.id)
                return@forEach // Always allow invites to the current server

            if (whitelistedGuilds.isNotEmpty() && targetGuild.id !in whitelistedGuilds)
                throw ViolationException("Invite `$invite` to ${targetGuild.name}")
            if (blacklistedGuilds.isNotEmpty() && targetGuild.id in blacklistedGuilds)
                throw ViolationException("Invite `$invite` to ${targetGuild.name}")
        }
    }


    fun resolve(jda: JDA, invite: String): Invite.Guild? {
        return try {
            Invite.resolve(jda, invite).complete()?.guild
        } catch (e: Exception) {
            null
        }
    }

}

/**
 * Rule for matching domains
 */
class DomainRule : CensorRule {
    private val urlRegex = Regex("(https?://[^\\s]+)")

    override fun check(message: Message, settings: CensorSetting) {
        if (!settings.domains.enabled)
            return

        val whitelist = settings.domains.whitelist.map { it.toLowerCase() }
        val blacklist = settings.domains.blacklist.map { it.toLowerCase() }
        urlRegex.findAll(message.contentRaw).map { it.value }.forEach {
            try {
                val url = URL(it)
                val domain = url.host.toLowerCase()
                if (whitelist.isNotEmpty() && domain !in whitelist) {
                    throw ViolationException("Not whitelisted domain: `$domain`")
                } else if (blacklist.isNotEmpty() && domain in blacklist) {
                    throw ViolationException("Blacklisted domain: `$domain`")
                }
            } catch (ignored: MalformedURLException) {
                // Ignore invalid URLs
            }
        }
    }
}