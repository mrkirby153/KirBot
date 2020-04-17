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

    private val availableModules = mutableListOf<Module>()
    val loadedModules = mutableListOf<Module>()

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

    fun load(registerListeners: Boolean = true) {
        val loadTime = measureTimeMillis {
            val reflections = Reflections("me.mrkirby153.KirBot")

            val modules = reflections.getSubTypesOf(Module::class.java)
            Bot.LOG.info("Discovered ${modules.size} modules")

            modules.forEach { Bot.applicationContext.registerLazySingleton(it) }

            Bot.LOG.info("Starting load of modules")

            // These modules should be loaded before the rest of the modules as these are considered
            // critical modules
            val eagerLoad = arrayOf(Database::class.java, Redis::class.java)
            modules.removeAll(eagerLoad)

            eagerLoad.forEach {
                Bot.applicationContext.get(it).load(registerListeners)
            }

            modules.forEach {
                Bot.applicationContext.get(it).load(registerListeners)
            }
        }
        Bot.LOG.info("Modules loaded in ${Time.format(1, loadTime)}")
    }

    fun startScheduler() {
        Bot.LOG.info("Starting periodic scheduler...")
        Bot.scheduler.scheduleAtFixedRate({
            val nextTick = ++tickCount
            loadedModules.forEach { mod ->
                mod.triggerPeriodicTasks(nextTick)
            }
        }, 0, 1, TimeUnit.SECONDS)
    }


    private fun resolveDependencies(module: Module, resolved: MutableList<Module>,
                                    seen: MutableList<Module> = mutableListOf()) {
        seen.add(module)
        module.dependencies.forEach { dep ->
            // Resolve the module into its instance
            val mod = availableModules.firstOrNull { it.javaClass == dep } ?: return@forEach
            if (mod !in resolved) {
                if (mod in seen)
                    throw IllegalStateException(
                            "Circular reference detected: ${module.name} -> ${mod.name}")
                resolveDependencies(mod, resolved, seen)
            }
        }
        if (module !in resolved)
            resolved.add(module)
    }
}