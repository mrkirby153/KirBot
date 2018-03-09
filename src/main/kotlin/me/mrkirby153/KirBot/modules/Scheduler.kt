package me.mrkirby153.KirBot.modules

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.gson.GsonBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.scheduler.InterfaceAdapter
import me.mrkirby153.KirBot.scheduler.Schedulable
import me.mrkirby153.kcutils.Time
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Scheduler : Module("scheduler") {

    private var schedulePrefix = "task:"
    private var scheduleList = "tasks"

    private val executor = Executors.newCachedThreadPool(
            ThreadFactoryBuilder().setDaemon(true).setNameFormat("schedule-executor-%d").build())

    private val timer = Executors.newScheduledThreadPool(1,
            ThreadFactoryBuilder().setDaemon(true).setNameFormat("executor_timer-%d").build())

    private val gson = GsonBuilder().apply {
        registerTypeAdapter(Schedulable::class.java, InterfaceAdapter<Schedulable>())
    }.create()

    override fun onLoad() {
        this.timer.scheduleAtFixedRate({
            try {
                this.process()
            } catch (ignored: Exception) {
            }
        }, 0L, 1, TimeUnit.SECONDS)
    }

    fun submit(schedulable: Schedulable, time: Long, unit: TimeUnit): String {
        val id = this.generateId()
        val convert = TimeUnit.MILLISECONDS.convert(time, unit)
        Bot.LOG.debug("Scheduling for ${Time.format(1, convert)}")

        val item = ScheduledItem(id, schedulable)
        val t = System.currentTimeMillis() + convert

        ModuleManager[Redis::class.java].getConnection().use {
            val key = schedulePrefix + id
            it.set(key, gson.toJson(item))
            it.zadd(scheduleList, t.toDouble(), key)
        }
        return id
    }

    fun cancel(id: String): Boolean {
        val key = schedulePrefix + id
        ModuleManager[Redis::class.java].getConnection().use {
            it.zrem(scheduleList, key)
            return it.del(key) > 0
        }
    }

    fun process() {
        ModuleManager[Redis::class.java].getConnection().use {
            val keys = it.zrangeByScore(scheduleList, "-inf", System.currentTimeMillis().toString())
            if (keys.size > 0) {
                Bot.LOG.debug("Processing keys $keys")
            } else {
                return
            }
            val toDel = mutableListOf<String>()
            keys.forEach { key ->
                val json = it.get(key) ?: return@forEach
                Bot.LOG.debug("Processing $json")
                val obj = gson.fromJson(json, ScheduledItem::class.java)
                this.executor.submit {
                    obj.schedulable.run()
                }
                toDel.add(key)
            }
            it.zrem(scheduleList, *toDel.toTypedArray())
            it.del(*toDel.toTypedArray())
        }
    }

    private fun generateId(): String {
        return UUID.randomUUID().toString().replace("-", "")
    }

    data class ScheduledItem(val id: String, val schedulable: Schedulable)
}