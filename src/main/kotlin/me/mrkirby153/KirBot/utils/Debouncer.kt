package me.mrkirby153.KirBot.utils

import net.dv8tion.jda.api.events.GenericEvent


class Debouncer(val timeout: Long = 120 * 1000) {

    private val debounces = mutableListOf<DebouncedEvent>()

    /**
     * Removes any expired debounce events
     */
    fun removeExpired() {
        debounces.removeIf { evt ->
            System.currentTimeMillis() > evt.expires
        }
    }

    /**
     * Finds a debounced event
     *
     * @param type The type of event
     * @param params The parameters to search for
     *
     * @return True if the event was debounced, false if it wasn't
     */
    fun find(type: Class<out GenericEvent>, vararg params: Pair<String, String?>): Boolean {
        val potentialEvents = this.debounces.filter { it.type == type }
        var found: DebouncedEvent? = null

        potentialEvents.forEach { evt ->
            var candidate = true
            params.forEach { param ->
                if (!evt.parameters[param.first].equals(param.second))
                    candidate = false
            }
            if (candidate)
                found = evt
        }
        if (found != null) {
            this.debounces.remove(found!!)
            if (found!!.expires < System.currentTimeMillis())
                return false
        }
        return found != null
    }

    fun create(type: Class<out GenericEvent>, vararg params: Pair<String, String?>) {
        val paramMap = mutableMapOf<String, String?>()
        params.forEach {
            paramMap[it.first] = it.second
        }
        this.debounces.add(DebouncedEvent(type, paramMap, System.currentTimeMillis() + timeout))
    }

    data class DebouncedEvent(val type: Class<out GenericEvent>,
                                      val parameters: Map<String, String?>,
                                      val expires: Long)
}