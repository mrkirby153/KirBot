package me.mrkirby153.KirBot.listener

import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.events.Event

object WaitUtils {

    var nextId = 1

    private val waitingEvents = mutableMapOf<Int, WaitingEvent<*>>()

    fun <T : Event> waitFor(clazz: Class<T>, callback: WaitingEvent<T>.(T) -> Unit): Int {
        val id = nextId++
        Bot.LOG.debug("Waiting for $clazz with ID $id")
        waitingEvents[id] = WaitingEvent(id, clazz, callback)
        return id
    }

    fun cancel(id: Int) {
        waitingEvents.remove(id)?.let {
            it.canceled = true
        }
    }

    fun process(event: Event) {
        val toRun = waitingEvents.filter { (_, evt) -> evt.eventClass == event.javaClass }.map { it.value }
        toRun.forEach {
            Bot.LOG.debug("Running ${it.id}")
            it.invoke(event)
        }
    }
}

class WaitingEvent<T : Event>(val id: Int, val eventClass: Class<T>,
                              private val callback: WaitingEvent<T>.(T) -> Unit) {

    var canceled = false

    fun invoke(event: Event) {
        if (canceled)
            return
        try {
            this.callback(event as T)
        } catch (e: Exception) {
            Bot.LOG.error("An error occurred when calling event [$id]", e)
        }
    }

    fun cancel() {
        Bot.LOG.debug("Canceling ${this.id}")
        WaitUtils.cancel(this.id)
    }
}