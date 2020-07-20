package com.mrkirby153.kirbot.services.command.context

/**
 * The minimum number that this parameter can be
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Min(val value: Long)

/**
 * The maximum number that this parameter can be
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Max(val value: Long)

/**
 * Marker annotation used in string parsing designating this parameter as not consuming the rest
 * of the arguments
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Single

/**
 * Annotation designating the name of this command argument. If absent, the method's name will be
 * used instead
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Parameter(val value: String)

/**
 * Marker annotation designating the field as optional. This should not be placed on nullable
 * fields
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Optional