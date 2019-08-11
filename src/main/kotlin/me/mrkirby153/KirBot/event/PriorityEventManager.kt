package me.mrkirby153.KirBot.event

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.stats.Statistics
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.http.HttpRequestEvent
import net.dv8tion.jda.api.hooks.IEventManager
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Similar to the default Annotation event manager with one minor difference: support for event
 * priorities, enforcing events being fired in a specific order.
 */
class PriorityEventManager : IEventManager {

    private val listeners: CopyOnWriteArrayList<Any> = CopyOnWriteArrayList()

    private val classMethods: ConcurrentHashMap<Any, CopyOnWriteArrayList<Method>> = ConcurrentHashMap()
    private val eventMethods: ConcurrentHashMap<Class<GenericEvent>, CopyOnWriteArrayList<EventListenerMethod>> = ConcurrentHashMap()

    private val queuedEvents: ConcurrentHashMap<String, CopyOnWriteArrayList<GenericEvent>> = ConcurrentHashMap()

    override fun handle(event: GenericEvent) {
        if(event !is HttpRequestEvent) {
            Statistics.eventType.labels(event.javaClass.name).inc()
        }

        // Walk up the hierarchy
        var current: Class<GenericEvent>? = event.javaClass
        while (current != null && current != Object::class.java) {
            val start = System.currentTimeMillis()
            dispatch(event, current)
            val end = System.currentTimeMillis()
            Statistics.eventDuration.labels(current.name).observe((end - start).toDouble())
            current = current.superclass as Class<GenericEvent>?
        }
    }

    fun onGuildReady(guild: Guild) {
        val queuedEvents = this.queuedEvents[guild.id] ?: return
        Bot.LOG.debug("Firing ${queuedEvents.size} queued events on $guild")
        queuedEvents.forEach {
            handle(it)
        }
    }

    override fun register(listener: Any) {
        this.listeners.add(listener)
        this.discoverEventListeners(listener)
    }

    override fun getRegisteredListeners(): MutableList<Any> {
        return this.listeners
    }

    override fun unregister(listener: Any) {
        this.listeners.remove(listener)
        val methods = this.classMethods.remove(listener) ?: return
        eventMethods.values.forEach {
            it.removeIf { it.method in methods }
        }
    }

    private fun dispatch(event: GenericEvent, evtType: Class<GenericEvent>) {
        val registeredEvents = this.eventMethods[evtType] ?: return
        registeredEvents.forEach { evt ->
            try {
                evt.method.invoke(evt.obj, event)
            } catch (e: InvocationTargetException) {
                Bot.LOG.error(
                        "The event listener ${evt.obj.javaClass.canonicalName}.${evt.method} encountered an error when processing ${event.javaClass.canonicalName}",
                        e)
            }
        }
    }

    /**
     * Discovers all event listener methods on a given class
     *
     * @param listener The listener to discover events on
     */
    private fun discoverEventListeners(listener: Any) {
        var registered = false
        listener.javaClass.declaredMethods.forEach {
            it.isAccessible = true
            if (it.parameterCount == 1) {
                if (GenericEvent::class.java.isAssignableFrom(it.parameterTypes.first())) {
                    if (!it.isAnnotationPresent(Subscribe::class.java)) {
                        Bot.LOG.warn(
                                "The method ${it.declaringClass.canonicalName}.${it.name} is missing the @Subscribe annotation")
                        return@forEach
                    } else {
                        val priority = it.getAnnotation(Subscribe::class.java).priority
                        Bot.LOG.debug(
                                "Registering ${it.declaringClass.canonicalName}.${it.name} to event ${it.parameterTypes.first()} with priority $priority")
                        val m = EventListenerMethod(it.parameterTypes.first() as Class<GenericEvent>,
                                priority, it, listener)
                        this.classMethods.getOrPut(listener) { CopyOnWriteArrayList() }.add(it)
                        this.eventMethods.getOrPut(m.event) { CopyOnWriteArrayList() }.add(m)
                        registered = true
                    }
                }
            }
        }
        this.eventMethods.values.forEach { events ->
            events.sortByDescending { it.priority.ordinal }
        }
        if (!registered)
            Bot.LOG.warn("${listener.javaClass.canonicalName} didn't contain any valid listeners!")
    }

    private data class EventListenerMethod(val event: Class<GenericEvent>, val priority: EventPriority,
                                           val method: Method, val obj: Any)
}