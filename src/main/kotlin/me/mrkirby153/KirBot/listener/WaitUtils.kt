package me.mrkirby153.KirBot.listener

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.GREEN_TICK
import me.mrkirby153.KirBot.utils.RED_TICK
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.Event
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

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

    fun process(event: GenericEvent) {
        val toRun = waitingEvents.filter { (_, evt) -> evt.eventClass == event.javaClass }.map { it.value }
        toRun.forEach {
            Bot.LOG.debug("Running ${it.id}")
            it.invoke(event)
        }
    }

    fun confirmYesNo(message: Message, user: User, ifYes: (() -> Unit)? = null,
                     ifNo: (() -> Unit)? = null) {
        message.addReaction(GREEN_TICK.emote!!).queue {
            message.addReaction(RED_TICK.emote!!).queue {
                waitFor(MessageReactionAddEvent::class.java) {
                    if (it.user != user || it.messageId != message.id)
                        return@waitFor
                    if (it.reactionEmote.isEmote) {
                        when (it.reactionEmote.id) {
                            RED_TICK.id -> {
                                ifNo?.invoke()
                                cancel()
                            }
                            GREEN_TICK.id -> {
                                ifYes?.invoke()
                                cancel()
                            }
                        }
                    }
                }
            }
        }
    }
}

class WaitingEvent<T : GenericEvent>(val id: Int, val eventClass: Class<T>,
                              private val callback: WaitingEvent<T>.(T) -> Unit) {

    var canceled = false

    fun invoke(event: GenericEvent) {
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