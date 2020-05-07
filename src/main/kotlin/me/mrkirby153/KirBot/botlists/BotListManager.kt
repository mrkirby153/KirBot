package me.mrkirby153.KirBot.botlists

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class BotListManager @Inject constructor(val shardManager: ShardManager) {

//    private val updateInterval = 3600 * 1000
    private val updateInterval = 300 * 1000

    private val registeredLists = mutableListOf<BotList>()

    private var pendingUpdate: Future<*>? = null
    private var nextRunAt: Long = 0

    fun registerBotList(list: BotList) {
        Bot.LOG.info("Registering bot list provider ${list.javaClass}")
        registeredLists.add(list)
    }

    fun updateBotLists(force: Boolean = false) {
        if(force) {
            Bot.LOG.debug("Force updating bot lists")
            doUpdate()
            nextRunAt = System.currentTimeMillis() + updateInterval
            Bot.LOG.debug("Next run in ${Time.format(1, nextRunAt - System.currentTimeMillis())}")
            return
        }
        if (System.currentTimeMillis() > nextRunAt) {
            // Update the lists immediately
            Bot.LOG.debug("Updating bot lists")
            doUpdate()
            nextRunAt = System.currentTimeMillis() + updateInterval
            Bot.LOG.debug("Next run in ${Time.format(1, nextRunAt - System.currentTimeMillis())}")
        } else {
            val pending = pendingUpdate
            if (pending != null && !pending.isDone) {
                Bot.LOG.debug("There already was a pending task, skipping")
                return // There's already a task that will update the bot list
            }
            val timeRemaining = nextRunAt - System.currentTimeMillis()
            Bot.LOG.debug("Scheduling bot list update in ${Time.format(1, timeRemaining)}")
            pendingUpdate = Bot.scheduler.schedule({
                Bot.LOG.debug("Delayed update of bot list")
                doUpdate()
                nextRunAt = System.currentTimeMillis() + updateInterval
                Bot.LOG.debug("Next run in ${Time.format(1, nextRunAt - System.currentTimeMillis())}")
            }, timeRemaining, TimeUnit.MILLISECONDS)
        }
    }

    private fun doUpdate() {
        val serverCount = shardManager.guilds.size
        registeredLists.forEach {
            Bot.LOG.debug("Updating list ${it.javaClass}")
            it.updateBotlist(serverCount)
        }
    }
}