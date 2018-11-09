package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.guild.GenericGuildEvent
import net.dv8tion.jda.core.hooks.EventListener
import net.dv8tion.jda.core.hooks.SubscribeEvent
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

open class KirBotEventListener : EventListener {

    private val eventMap = mutableMapOf<Class<*>, MutableList<Method>>()

    init {
        discoverEvents()
    }


    override fun onEvent(event: Event) {
        if (event is GenericGuildEvent) {
            if (event.guild.kirbotGuild.isAvailable && event.guild.kirbotGuild.ready) {
                invokeEvent(event)
            }
            return
        }
        invokeEvent(event)
    }

    private fun discoverEvents() {
        Bot.LOG.debug("Discovering events in ${this.javaClass.canonicalName}")
        var registeredEventListeners = 0
        this.javaClass.declaredMethods.forEach {
            it.isAccessible = true
            if (it.parameterCount == 1) {
                if (Event::class.java.isAssignableFrom(it.parameterTypes.first())) {
                    if (!it.isAnnotationPresent(SubscribeEvent::class.java)) {
                        Bot.LOG.warn(
                                "The method ${it.declaringClass.canonicalName}.${it.name} is missing the @SubscribeEvent annotation")
                        return@forEach
                    } else {
                        Bot.LOG.debug(
                                "Registering ${it.declaringClass.canonicalName}.${it.name} to event ${it.parameterTypes.first()}")
                        eventMap.getOrPut(it.parameterTypes.first()) { mutableListOf() }.add(it)
                        registeredEventListeners += 1
                    }
                }
            }
        }
        Bot.LOG.debug(
                "Registered $registeredEventListeners events in ${this.javaClass.canonicalName}")
    }

    private fun invokeEvent(event: Event) {
        this.eventMap[event.javaClass]?.forEach { evt ->
            try {
                evt.invoke(this, event)
            } catch (e: InvocationTargetException) {
                e.printStackTrace()
                Bot.LOG.error(
                        "The event listener ${evt.declaringClass.canonicalName}.${evt.name} encountered an error")
            }
        }
    }
}