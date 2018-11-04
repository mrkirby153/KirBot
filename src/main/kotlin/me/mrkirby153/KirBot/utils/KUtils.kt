package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.listener.WaitUtils
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.kcutils.Time
import me.xdrop.fuzzywuzzy.FuzzySearch
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

fun uploadToArchive(text: String, ttl: Int = 604800): String {
    ModuleManager[Redis::class.java].getConnection().use {
        val key = UUID.randomUUID().toString()
        Bot.LOG.debug("Archive created $key (Expires in ${Time.formatLong(ttl * 1000L,
                Time.TimeUnit.SECONDS).toLowerCase()})")
        it.set("archive:$key", text)
        it.expire("archive:$key", ttl)
        return String.format(Bot.properties.getProperty("archive-base"), key)
    }
}

fun promptForConfirmation(context: Context, msg: String, onConfirm: (() -> Boolean)? = null,
                          onDeny: (() -> Boolean)? = null) {
    context.channel.sendMessage(msg).queue { m ->
        m.addReaction(GREEN_TICK.emote).queue()
        m.addReaction(RED_TICK.emote).queue()
        WaitUtils.waitFor(MessageReactionAddEvent::class.java) {
            if (it.user.id != context.author.id)
                return@waitFor
            if (it.messageId != m.id)
                return@waitFor
            if (it.reactionEmote.isEmote) {
                when (it.reactionEmote.id) {
                    GREEN_TICK.id -> {
                        if (onConfirm == null || onConfirm.invoke())
                            m.delete().queue()
                        cancel()
                    }
                    RED_TICK.id -> {
                        if (onDeny == null || onDeny.invoke())
                            m.delete().queue()
                        cancel()
                    }
                }
            }
        }
    }
}

fun convertSnowflake(snowflake: String): Date {
    val s = snowflake.toLong()
    val time = s.shr(22)
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = time + 1420070400000
    return calendar.time
}

@JvmOverloads
fun makeEmbed(title: String?, msg: String?, color: Color? = Color.WHITE, img: String? = null,
              thumb: String? = null, author: User? = null): MessageEmbed {
    return EmbedBuilder().run {
        setDescription(msg)
        setTitle(title, null)
        setColor(color)

        if (author != null) {
            setAuthor(author.name, null, author.avatarUrl)
        }
        setThumbnail(thumb)
        setImage(img)
        build()
    }
}

fun localizeTime(time: Int): String {
    return when {
        time < 60 -> "$time seconds"
        time < 3600 -> "${roundTime(2, time.toDouble() / 60)} minutes"
        time < 86400 -> "${(roundTime(2, time.toDouble() / 3600))} hours"
        time < 604800 -> "${roundTime(2, time.toDouble() / 86400)} days"
        else -> "${roundTime(2, time.toDouble() / 604800)} weeks"
    }
}

fun roundTime(degree: Int, number: Double): Double {
    if (degree == 0)
        return Math.round(number).toDouble()
    var format = "#.#"
    for (i in (1 until degree))
        format += "#"

    val sym = DecimalFormatSymbols(Locale.US)
    val twoDform = DecimalFormat(format, sym)
    return twoDform.format(number).toDouble()
}

infix fun Any.botUrl(url: String): String {
    return Bot.constants.getProperty("bot-base-url") + "/" + url
}

/**
 * Fuzzy matches an item out of a list given a query string
 *
 * @param items The list of items to search
 * @param query The query string to search for in the item
 * @param mapper A function that maps the items to strings
 * @param ratio The ratio that the items must match the query string
 * @param minDifference The minimum difference in ratios between the first and second candidate (if any)
 *
 * @throws FuzzyMatchException.TooManyMatchesException If more than one item is candidate for the string
 * @throws FuzzyMatchException.NoMatchesException If no items match the query string
 *
 * @return The item that most closely matches the query string
 */
fun <T> fuzzyMatch(items: List<T>, query: String, mapper: (T) -> String = { it.toString() },
                   ratio: Int = 40, minDifference: Int = 20): T {
    val exactMatches = items.filter { i ->
        mapper.invoke(i) == query
    }
    if (exactMatches.isNotEmpty()) {
        if (exactMatches.size > 1)
            throw FuzzyMatchException.TooManyMatchesException()
        return exactMatches.first()
    } else {
        val fuzzyRated = mutableMapOf<T, Int>()
        items.forEach { i ->
            fuzzyRated[i] = FuzzySearch.partialRatio(query, mapper.invoke(i))
        }
        val matches = fuzzyRated.entries.sortedBy { it.value }.reversed().filter { it.value > ratio }
        if (matches.isEmpty())
            throw FuzzyMatchException.NoMatchesException()
        val first = matches.first()
        return when {
            // Only 1 item matched the criteria
            matches.size == 1 -> first.key
            // The 2nd item has a ratio that is less than the minDifference
            first.value - matches[1].value > minDifference -> first.key
            // We've failed the previous two conditions, we can't determine the role
            else -> throw FuzzyMatchException.TooManyMatchesException()
        }
    }
}

/**
 * An exception thrown when an error occurs when [Matching Strings][fuzzyMatch]
 */
open class FuzzyMatchException : Exception() {
    /**
     * An exception thrown when too many items match the given query string
     */
    class TooManyMatchesException : FuzzyMatchException()

    /**
     * An exception thrown when no items match the given query
     */
    class NoMatchesException : FuzzyMatchException()
}