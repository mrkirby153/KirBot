package com.mrkirby153.kirbot.services.command.context

/**
 * The minimum number that this parameter can be
 */
annotation class Min(val value: Long)

/**
 * The maximum number that this parameter can be
 */
annotation class Max(val value: Long)

/**
 * Marker annotation used in string parsing designating this parameter as not consuming the rest
 * of the arguments
 */
annotation class Single