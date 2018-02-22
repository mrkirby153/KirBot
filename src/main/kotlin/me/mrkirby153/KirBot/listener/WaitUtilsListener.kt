package me.mrkirby153.KirBot.listener

import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.EventListener

class WaitUtilsListener : EventListener {

    override fun onEvent(event: Event) {
        WaitUtils.process(event)
    }
}