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

/**
 * Annotation designating the name of this command argument. If absent, the method's name will be
 * used instead
 */
annotation class Parameter(val value: String)

/**
 * Marker annotation designating the field as optional. This should not be placed on nullable
 * fields
 */
annotation class Optional