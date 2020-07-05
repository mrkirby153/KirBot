package com.mrkirby153.kirbot.services.impl

import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import net.dv8tion.jda.api.events.GenericEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher

internal class JdaEventManagerTest {

    @MockK
    lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var manager: JdaEventManager

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        manager = JdaEventManager(eventPublisher)
    }

    @Test
    fun testEventHandler() {
        val event = mockk<GenericEvent>()
        manager.onEvent(event)
        verify { eventPublisher.publishEvent(event) }
    }
}