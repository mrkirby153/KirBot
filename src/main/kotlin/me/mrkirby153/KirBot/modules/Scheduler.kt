package me.mrkirby153.KirBot.modules

import com.google.gson.GsonBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.scheduler.InterfaceAdapter
import me.mrkirby153.KirBot.scheduler.Schedulable
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import org.json.JSONObject
import org.json.JSONTokener
import java.util.UUID
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.roundToLong

class Scheduler : Module("scheduler") {

    private var keyFormat = "task:%s"
    private var list = "tasks"

    private var nextTask: ScheduledFuture<*>? = null
    private var nextTaskId: String? = null

    private val gson = GsonBuilder().apply {
        registerTypeAdapter(Schedulable::class.java, InterfaceAdapter<Schedulable>())
    }.create()

    init {
        dependencies.add(Redis::class.java)
    }

    override fun onLoad() {
        this.queueTasks()
    }

    fun submit(schedulable: Schedulable, time: Long, unit: TimeUnit): String {
        val id = this.generateId()
        val convert = TimeUnit.MILLISECONDS.convert(time, unit)

        val item = ScheduledItem(id, schedulable)
        val t = System.currentTimeMillis() + convert

        ModuleManager[Redis::class.java].getConnection().use {
            val key = keyFormat.format(id)
            it.set(key, gson.toJson(item))
            it.zadd(list, t.toDouble(), key)
        }
        queueTasks()
        return id
    }

    fun cancel(id: String): Boolean {
        val key = keyFormat.format(id)
        ModuleManager[Redis::class.java].getConnection().use {
            it.zrem(list, key)
            return it.del(key) > 0
        }
    }

    private fun runOldTasks() {
        Bot.LOG.debug("Running all expired tasks...")
        // Run expired tasks
        ModuleManager[Redis::class.java].getConnection().use { jedis ->
            val keys = jedis.zrangeByScore(list, "-inf",
                    (System.currentTimeMillis() + 100).toString())
            val toRemove = mutableListOf<String>()
            keys.forEach { key ->
                val task = jedis.get(key)
                if (task == null) {
                    toRemove.add(key)
                    return@forEach
                }
                val scheduledItem = gson.fromJson(task, ScheduledItem::class.java)
                val future = Bot.scheduler.submit {
                    Bot.LOG.debug("Running task ${scheduledItem.id}")
                    scheduledItem.schedulable.run()
                }
                try {
                    future.get(2, TimeUnit.SECONDS)
                } catch (e: TimeoutException) {
                    Bot.LOG.warn("Task ${scheduledItem.id} timed out when executing")
                    // Ignore
                }
                jedis.zrem(list, key)
                jedis.del(key)
            }
        }
    }

    private fun queueTasks() {
        Bot.LOG.debug("Queueing tasks")
        ModuleManager[Redis::class.java].getConnection().use { jedis ->
            val nextKey = jedis.zrangeByScore(list, System.currentTimeMillis().toString(),
                    "+inf", 0, 1)?.firstOrNull()
            if (nextKey == null) {
                Bot.LOG.debug("No more tasks left to run")
                return
            }
            if (nextKey == this.nextTaskId) {
                Bot.LOG.debug("Not re-scheduling the next task as it's the same")
                return
            } else {
                if (nextTask != null) {
                    Bot.LOG.debug("Re-scheduling $nextTaskId -- $nextKey runs shorter")
                    nextTask?.cancel(false)
                    reset()
                }
            }

            val nextKeyScore = jedis.zscore(list, nextKey)

            val runIn = nextKeyScore.roundToLong() - System.currentTimeMillis()
            Bot.LOG.debug(
                    "Next task: $nextKey in ${Time.formatLong(runIn, Time.TimeUnit.MILLISECONDS)}")

            nextTaskId = nextKey
            nextTask = Bot.scheduler.schedule({
                runOldTasks()
                nextTask = null
                nextTaskId = null
                queueTasks()
            }, runIn, TimeUnit.MILLISECONDS)
        }
    }

    private fun reset() {
        nextTaskId = null
        nextTask = null
    }

    private fun generateId(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    data class ScheduledItem(val id: String, val schedulable: Schedulable)
}


class SchedulerStats{

    @Command(name = "sstats")
    @AdminCommand
    fun execute(context: Context, cmdContext: CommandContext) {
        context.send().embed("Scheduler Statistics") {
            description {
                +"Current scheduler statistics"
            }
            fields {
                ModuleManager[Redis::class.java].getConnection().use { jedis ->
                    // Next Task
                    val pendingTasks = jedis.zrangeByScore("tasks",
                            System.currentTimeMillis().toString(), "+inf")
                    field {
                        title = "Pending tasks"
                        description = pendingTasks.size.toString()
                        inline = true
                    }
                    field {
                        inline = true
                        title = "Expired tasks"
                        description = jedis.zrangeByScore("tasks", "-inf",
                                (System.currentTimeMillis() + 100).toString()).size.toString()
                    }
                    field {
                        title = "Next task"
                        description = pendingTasks.firstOrNull() ?: "No tasks scheduled."
                        inline = true
                    }
                    if (pendingTasks.firstOrNull() != null) {
                        field {
                            inline = true
                            title = "Running in"
                            description = Time.formatLong(jedis.zscore("tasks",
                                    pendingTasks.first()).roundToLong() - System.currentTimeMillis(),
                                    Time.TimeUnit.MILLISECONDS)
                        }
                        field {
                            inline = true
                            title = "Class name"
                            description {
                                val json = JSONObject(JSONTokener(jedis.get(pendingTasks.first())))
                                val scheduler = json.getJSONObject("schedulable")
                                appendln(scheduler.getString("type"))
                            }
                        }
                    }
                }
            }
            footer {
                text {
                    +"Requested by ${context.author.nameAndDiscrim}"
                }
            }
            timestamp {
                now()
            }
        }.rest().queue()
    }

}