package com.mrkirby153.kirbot.services.impl

import com.mrkirby153.kirbot.services.JdaEventService
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.hooks.EventListener
import org.apache.logging.log4j.LogManager
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class JdaEventManager(private val eventPublisher: ApplicationEventPublisher) : JdaEventService,
        EventListener {

    private val log = LogManager.getLogger()


    override fun onEvent(event: GenericEvent) {
        log.debug("Relaying $event to the spring application context")
        eventPublisher.publishEvent(event)
    }
}