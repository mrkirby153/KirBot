package com.mrkirby153.kirbot.utils

import java.util.Calendar
import java.util.Date

private const val DISCORD_EPOCH = 1420070400000

/**
 * Converts the provided [snowflake] to a [Date]
 */
fun convertSnowflake(snowflake: String): Date {
    val s = snowflake.toLong()
    val time = s.shr(22)
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = time + DISCORD_EPOCH
    return calendar.time
}

/**
 * Converts the provided [date] to a Snowflake
 */
fun toSnowflake(date: Date): String {
    return (date.toInstant().toEpochMilli() - DISCORD_EPOCH).shl(22).toString()
}