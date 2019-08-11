package me.mrkirby153.KirBot.listener

import me.mrkirby153.KirBot.event.Subscribe
import net.dv8tion.jda.api.events.Event

class WaitUtilsListener {

    @Subscribe
    fun onEvent(event: Event) {
        WaitUtils.process(event)
    }
}