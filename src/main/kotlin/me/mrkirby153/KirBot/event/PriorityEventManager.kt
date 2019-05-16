package me.mrkirby153.KirBot.event

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.stats.Statistics
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.http.HttpRequestEvent
import net.dv8tion.jda.core.hooks.IEventManager
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
    private val eventMethods: ConcurrentHashMap<Class<Event>, CopyOnWriteArrayList<EventListenerMethod>> = ConcurrentHashMap()

    private val queuedEvents: ConcurrentHashMap<String, CopyOnWriteArrayList<Event>> = ConcurrentHashMap()

    override fun handle(event: Event) {
        if(event !is HttpRequestEvent) {
            Statistics.eventType.labels(event.javaClass.name).inc()
        }
//        if (event is GenericGuildEvent && Bot.state == BotState.RUNNING) {
//            if(event !is GuildJoinEvent) { // Ensure the guild join event is passed through
//                // If the event is a guild event, check if the guild is ready
//                if (!event.guild.kirbotGuild.ready) {
//                    val arrayList = queuedEvents.computeIfAbsent(
//                            event.guild.id) { CopyOnWriteArrayList() }
//                    arrayList.add(event)
//                    Bot.LOG.debug(
//                            "Queueing ${event.javaClass} for guild ${event.guild}. Reason: Guild not ready")
//                    return
//                }
//            }
//        }

        // Walk up the hierarchy
        var current: Class<Event>? = event.javaClass
        while (current != null && current != Object::class.java) {
            val start = System.currentTimeMillis()
            dispatch(event, current)
            val end = System.currentTimeMillis()
            Statistics.eventDuration.labels(current.name).observe((end - start).toDouble())
            current = current.superclass as Class<Event>?
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

    private fun dispatch(event: Event, evtType: Class<Event>) {
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
                if (Event::class.java.isAssignableFrom(it.parameterTypes.first())) {
                    if (!it.isAnnotationPresent(Subscribe::class.java)) {
                        Bot.LOG.warn(
                                "The method ${it.declaringClass.canonicalName}.${it.name} is missing the @Subscribe annotation")
                        return@forEach
                    } else {
                        val priority = it.getAnnotation(Subscribe::class.java).priority
                        Bot.LOG.debug(
                                "Registering ${it.declaringClass.canonicalName}.${it.name} to event ${it.parameterTypes.first()} with priority $priority")
                        val m = EventListenerMethod(it.parameterTypes.first() as Class<Event>,
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

    private data class EventListenerMethod(val event: Class<Event>, val priority: EventPriority,
                                           val method: Method, val obj: Any)
}