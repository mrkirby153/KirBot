package me.mrkirby153.KirBot.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

/**
 * Handle conversions from human-readable times and computer readbale times
 */
object Time {

    val DATE_FORMAT_NOW = "MM-dd-yy HH:mm:ss"
    val DATE_FORMAT_DAY = "MM-dd-yy"

    /**
     * Convert milliseconds to the specified time unit

     * @param trim The amount of decimal places
     * *
     * @param time The time
     * *
     * @param type The time unit tot convert to
     * *
     * @return The converted time
     */
    fun convert(trim: Int, time: Long, type: TimeUnit): Double {
        var t = type
        if (t == TimeUnit.FIT) {
            if (time < 60000)
                t = TimeUnit.SECONDS
            else if (time < 3600000)
                t = TimeUnit.MINUTES
            else if (time < 86400000)
                t = TimeUnit.HOURS
            else
                t = TimeUnit.DAYS
        }

        if (t == TimeUnit.DAYS) return trim(trim, time / 86400000.0)
        if (t == TimeUnit.HOURS) return trim(trim, time / 3600000.0)
        if (t == TimeUnit.MINUTES) return trim(trim, time / 60000.0)
        if (t == TimeUnit.SECONDS)
            return trim(trim, time / 1000.0)
        else
            return time.toDouble()
    }

    /**
     * Gets a String representing the current date in the format: <pre>MM-dd-yy</pre>

     * @return The date
     */
    fun date(): String {
        return SimpleDateFormat(DATE_FORMAT_DAY).format(Calendar.getInstance().time)
    }

    /**
     * Formats milliseconds into human readable format

     * @param trim The amount of decimal places
     * *
     * @param time The time
     * *
     * @param type The time unit to display in
     * *
     * @return A string in human-readable format
     */
    fun format(trim: Int, time: Long, type: TimeUnit): String {
        var t = type
        if (time == -1L) return "Permanent"

        if (t == TimeUnit.FIT) {
            if (time < 1000)
                t = TimeUnit.MILLISECONDS
            else if (time < 60000)
                t = TimeUnit.SECONDS
            else if (time < 3600000)
                t = TimeUnit.MINUTES
            else if (time < 86400000)
                t = TimeUnit.HOURS
            else
                t = TimeUnit.DAYS
        }

        val text: String
        if (t == TimeUnit.DAYS)
            text = trim(trim, time / 86400000.0).toString() + " Days"
        else if (t == TimeUnit.HOURS)
            text = trim(trim, time / 3600000.0).toString() + " Hours"
        else if (t == TimeUnit.MINUTES)
            text = trim(trim, time / 60000.0).toString() + " Minutes"
        else if (t == TimeUnit.SECONDS)
            text = trim(trim, time / 1000.0).toString() + " Seconds"
        else
            text = trim(0, time.toDouble()).toString() + " Milliseconds"

        return text
    }

    /**
     * Gets a String representing the current time in the format: <pre>MM-dd-yy HH:mm:ss</pre>

     * @return The date
     */
    fun now(): String {
        return SimpleDateFormat(DATE_FORMAT_NOW).format(Calendar.getInstance().time)
    }

    /**
     * Trims the double to the specified number of decimal places

     * @param degree The quantity of decimal places
     * *
     * @param d      The double to trim
     * *
     * @return A trimmed double
     */
    fun trim(degree: Int, d: Double): Double {
        if (degree == 0) {
            return Math.round(d).toDouble()
        }
        var format = "#.#"
        for (i in 1..degree - 1) {
            format += "#"
        }
        val symb = DecimalFormatSymbols(Locale.US)
        val twoDForm = DecimalFormat(format, symb)
        return java.lang.Double.valueOf(twoDForm.format(d))
    }

    enum class TimeUnit {
        FIT,
        DAYS,
        HOURS,
        MINUTES,
        SECONDS,
        MILLISECONDS
    }

}