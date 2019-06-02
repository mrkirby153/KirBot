package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.listener.WaitUtils
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.kcutils.Time
import me.xdrop.fuzzywuzzy.FuzzySearch
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import okhttp3.Request
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.imageio.ImageIO

/**
 * Uploads a string to the bot archive
 *
 * @param text The text to upload to the archive
 * @param ttl The time (in seconds) that the archive should exist for
 *
 * @return A URL to the archive
 */
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

/**
 * Prompt the user for confirmation before performing an action
 *
 * @param context The context
 * @param msg The message to send to the user
 * @param onConfirm The action to run when confirmed
 * @param onDeny The action to run when denied
 */
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

/**
 * Converts a snowflake to a [Date]
 *
 * @param snowflake The snowflake to convert
 *
 * @return The [Date]
 */
fun convertSnowflake(snowflake: String): Date {
    val s = snowflake.toLong()
    val time = s.shr(22)
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = time + 1420070400000
    return calendar.time
}

fun toSnowflake(date: Date): String {
    return (date.toInstant().toEpochMilli() - 1420070400000).shl(22).toString()
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
 * Gets the primary color in an image
 *
 * @param url The url
 * @return The primary color
 */
fun getPrimaryColor(url: String): Color {
    val req = Request.Builder().url(url).build()
    val resp = HttpUtils.CLIENT.newCall(req).execute()
    if (resp.code() != 200)
        throw IllegalArgumentException("Received non-success response code ${resp.code()}")
    val img = ImageIO.read(resp.body()!!.byteStream())
    return getPrimaryColor(img)
}

/**
 * Gets the primary color in an image
 *
 * @param image The imag
 * @return The primary color
 */
fun getPrimaryColor(image: BufferedImage): Color {
    val freq = mutableMapOf<Int, Int>()
    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            val f = freq[image.getRGB(x, y)] ?: 0
            freq[image.getRGB(x, y)] = f + 1
        }
    }
    var max = freq.entries.first().key
    var cnt = freq.entries.first().value
    freq.forEach { k, v ->
        if (v > cnt) {
            max = k
            cnt = v
        }
    }
    return Color(max)
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