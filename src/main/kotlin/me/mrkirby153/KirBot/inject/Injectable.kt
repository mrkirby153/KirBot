package me.mrkirby153.KirBot.inject

/**
 * Marker annotation declaring this class as injectable and it should automatically be added to
 * the context when the application starts
 */
@Target(AnnotationTarget.CLASS)
annotation class Injectable(val name: String = "")