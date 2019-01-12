package me.mrkirby153.KirBot.event

/**
 * The order events will be executed in. If two events have the same priority, ties will be broken
 * arbitrarily.
 *
 * Events are executed `HIGHEST` priority to `LOWEST` priority
 */
enum class EventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST
}