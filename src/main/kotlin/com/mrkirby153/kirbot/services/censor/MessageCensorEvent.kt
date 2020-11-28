package com.mrkirby153.kirbot.services.censor

import net.dv8tion.jda.api.entities.Message

/**
 * An event fired when a message is censored
 */
data class MessageCensorEvent(val msg: Message, val violations: List<ViolationException>,
                              val deleted: Boolean)