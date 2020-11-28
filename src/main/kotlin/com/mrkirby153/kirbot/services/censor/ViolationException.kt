package com.mrkirby153.kirbot.services.censor

/**
 * A violation exception, thrown when a message violates a [CensorRule]
 */
class ViolationException(val msg: String): Exception(msg);