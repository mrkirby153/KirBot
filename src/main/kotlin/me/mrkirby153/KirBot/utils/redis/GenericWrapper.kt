package me.mrkirby153.KirBot.utils.redis

/**
 * Class to wrap generics because Gson isn't very good at serializing generics (Read: absolutely terrible)
 */
open class GenericWrapper<out T>(val obj: T)