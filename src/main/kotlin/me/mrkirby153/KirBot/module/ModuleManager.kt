package me.mrkirby153.KirBot.module

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.kcutils.Time
import org.reflections.Reflections
import kotlin.reflect.KClass
import kotlin.system.measureTimeMillis

object ModuleManager {

    private val availableModules = mutableListOf<Module>()

    val loadedModules = mutableListOf<Module>()

    fun <T : Module> getLoadedModule(clazz: Class<T>): T? {
        return loadedModules.firstOrNull { it.javaClass == clazz } as? T
    }

    fun <T : Module> getModule(clazz: Class<T>): T? {
        return availableModules.firstOrNull { it.javaClass == clazz } as? T
    }

    operator fun <T : Module> get(clazz: Class<T>): T = getLoadedModule(clazz)
            ?: throw IllegalArgumentException("The provided module wasn't loaded")

    operator fun <T : Module> get(clazz: KClass<T>): T = get(clazz.java)

    fun loadModules(registerListeners: Boolean = true) {
        val startTime = System.currentTimeMillis()
        Bot.LOG.info("Module Manager Starting Up...")
        // Populate the available modules
        val reflections = Reflections("me.mrkirby153.KirBot")

        val modules = reflections.getSubTypesOf(Module::class.java)

        Bot.LOG.info("Found ${modules.size} modules")
        modules.forEach { availableModules.add(it.newInstance()) }

        // Dependency resolution
        Bot.LOG.debug("Beginning module dependency resolution")
        val resolved = mutableListOf<Module>()
        val depResolveTime = measureTimeMillis {
            availableModules.forEach {
                resolveDependencies(it, resolved)
            }
        }
        Bot.LOG.debug("Dependencies resolved in ${Time.format(2, depResolveTime)}")

        Bot.LOG.debug("Load order:")
        resolved.forEachIndexed { index, mod ->
            Bot.LOG.debug("  ${index + 1} - $mod")
        }

        // Beginning load of module
        Bot.LOG.debug("Loading modules...")
        resolved.forEach {
            it.load(registerListeners)
            loadedModules.add(it)
        }
        Bot.LOG.info("Modules loaded in ${Time.format(1, System.currentTimeMillis() - startTime)}")
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