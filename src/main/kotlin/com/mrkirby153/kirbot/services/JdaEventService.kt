package com.mrkirby153.kirbot.services

import net.dv8tion.jda.api.events.GenericEvent

/**
 * Shim service to handle translation from JDA events to Spring events
 */
interface JdaEventService {

    /**
     * Relay a [GenericEvent] from discord to the spring context
     *
     * @param event the event to relay
     */
    fun onEvent(event: GenericEvent)

}