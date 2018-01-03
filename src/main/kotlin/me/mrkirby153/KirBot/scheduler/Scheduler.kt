package me.mrkirby153.KirBot.scheduler

import com.google.common.util.concurrent.ThreadFactoryBuilder
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.kcutils.Time
import java.util.Random
import java.util.SortedMap
import java.util.TreeMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Scheduler {

    var schedule: SortedMap<Long, ScheduledItem> = TreeMap()

    val executor = Executors.newCachedThreadPool(
            ThreadFactoryBuilder().setDaemon(true).setNameFormat("schedule-executor-%d").build())

    val timer = Executors.newScheduledThreadPool(1,
            ThreadFactoryBuilder().setDaemon(true).setNameFormat("executor_timer-%d").build())

    private val gson = GsonBuilder().apply {
        registerTypeAdapter(Schedulable::class.java, InterfaceAdapter<Schedulable>())
    }.create()
    private val random = Random()

    init {
        timer.scheduleAtFixedRate({
            try {
                this.process()
            } catch (ignored: Exception) {
            }
        }, 0L, 10L, TimeUnit.MILLISECONDS)
    }

    fun submit(schedulable: Schedulable, time: Long, unit: TimeUnit): String {
        val id = generateId()
        val convert = TimeUnit.MILLISECONDS.convert(time, unit)
        Bot.LOG.debug("Scheduling for ${Time.format(1, convert)}")
        schedule.put(System.currentTimeMillis() + convert,
                ScheduledItem(id, schedulable))
        save()
        return id
    }

    fun cancel(id: String): Boolean {
        val removed = schedule.entries.removeIf { it.value.id == id }
        save()
        return removed
    }

    fun process() {
        schedule.forEach {
            if (it.key < System.currentTimeMillis()) {
                Bot.LOG.debug("Executing ${it.value.id}")
                executor.submit({
                    it.value.schedulable.run()
                })
                cancel(it.value.id)
            } else {
                return@forEach
            }
        }
    }

    fun save() {
        Bot.files.schedule.outputStream().use { outputStream ->
            val json = gson.toJson(schedule)
            outputStream.write(json.toByteArray())
            outputStream.flush()
        }
    }

    fun load() {
        Bot.files.schedule.inputStream().use {
            it.reader().use {
                schedule = gson.fromJson(it,
                        object : TypeToken<SortedMap<Long, ScheduledItem>>() {}.type)
            }
        }
    }

    private fun generateId(): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
        return buildString {
            for (i in 0 until 5) {
                append(characters[random.nextInt(characters.length)])
            }
        }
    }

    data class ScheduledItem(val id: String, val schedulable: Schedulable)
}