package me.mrkirby153.KirBot.module

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.modules.Database
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.kcutils.Time
import org.reflections.Reflections
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

object ModuleManager {

    private val discoveredModules = mutableListOf<Class<out Module>>()
    private var discovered = false
    private var tickCount = 0

    @Deprecated("Use dependency injection instead or retrieve from application context",
            ReplaceWith("Bot.applicationContext.get(clazz)", "me.mrkirby153.KirBot.Bot"))
    fun <T : Module> getLoadedModule(clazz: Class<T>): T? {
        return Bot.applicationContext.get(clazz)
    }

    @Deprecated("Use Dependency injection instead or retrieve from application context",
            ReplaceWith("Bot.applicationContext.get(clazz)", "me.mrkirby153.KirBot.Bot"))
    operator fun <T : Module> get(clazz: Class<T>): T = getLoadedModule(clazz)
            ?: throw IllegalArgumentException("The provided module wasn't loaded")

    @Deprecated("Use dependency injection instead or retrieve from application context",
            ReplaceWith("Bot.applicationContext.get(clazz)", "me.mrkirby153.KirBot.Bot"))
    operator fun <T : Module> get(clazz: KClass<T>): T = get(clazz.java)

    fun discoverModules() {
        if (discovered)
            throw IllegalStateException("Attempting to discover modules when already discovered")
        val reflections = Reflections("me.mrkirby153.KirBot")
        val modules = reflections.getSubTypesOf(Module::class.java)
        Bot.LOG.info("Discovered ${modules.size} modules")

        modules.forEach { Bot.applicationContext.registerLazySingleton(it) }
        discoveredModules.addAll(modules)
        discovered = true
    }

    fun load(registerListeners: Boolean = true) {
        val loadTime = measureTimeMillis {
            Bot.LOG.info("Starting load of modules")

            if(!discovered)
                discoverModules()

            // These modules should be loaded before the rest of the modules as these are considered
            // critical modules
            val toLoad = mutableListOf(*discoveredModules.toTypedArray())
            val eagerLoad = arrayOf(Database::class.java, Redis::class.java)
            toLoad.removeAll(eagerLoad)

            eagerLoad.forEach {
                Bot.applicationContext.get(it).load(registerListeners)
            }

            toLoad.forEach {
                Bot.applicationContext.get(it).load(registerListeners)
            }
        }
        Bot.LOG.info("Modules loaded in ${Time.format(1, loadTime)}")
    }

    fun startScheduler() {
        Bot.LOG.info("Starting periodic scheduler...")
        Bot.scheduler.scheduleAtFixedRate({
            val nextTick = ++tickCount
            discoveredModules.forEach { mod ->
                Bot.applicationContext.get(mod).triggerPeriodicTasks(nextTick)
            }
        }, 0, 1, TimeUnit.SECONDS)
    }

    fun unloadAll() {
        discoveredModules.forEach { mod ->
            Bot.applicationContext.get(mod).unload(true)
        }
    }
}