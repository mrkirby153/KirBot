package me.mrkirby153.KirBot.module

import me.mrkirby153.KirBot.Bot
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

abstract class Module(val name: String) {

    var loaded = false

    val dependencies = mutableListOf<Class<out Module>>()

    private val periodicTasks = mutableMapOf<Method, Int>()

    abstract fun onLoad()

    open fun onUnload() {}


    fun load(registerListeners: Boolean = true) {
        log("Starting load")
        debug("Calling onLoad()")
        onLoad()
        debug("Registering listener")
        if (registerListeners)
            Bot.shardManager.addEventListener(this)
        debug("Registering periodic tasks")
        this.javaClass.declaredMethods.filter {
            it.getAnnotation(Periodic::class.java) != null
        }.forEach { method ->
            if (method.parameterCount > 0) {
                log("Method ${method.name} has parameters. Periodic tasks must not have parameters")
                return@forEach
            }
            method.isAccessible = true
            periodicTasks[method] = method.getAnnotation(Periodic::class.java).interval
        }
        if (periodicTasks.count() > 0)
            debug("Registered ${periodicTasks.count()} periodic tasks")
        debug("Load complete")
        loaded = true
    }

    fun unload(unregisterListener: Boolean = true) {
        log("Starting unload")
        if (!loaded) {
            throw IllegalStateException(
                    "Attempting to unload a module that has already been unloaded")
        }
        debug("Calling onUnload()")
        onUnload()
        debug("Removing listener")
        if (unregisterListener)
            Bot.shardManager.removeEventListener(this)
        periodicTasks.clear()
        log("Unloading complete")
        loaded = false
    }

    fun log(message: Any) {
        Bot.LOG.info("[${name.toUpperCase()}] $message")
    }

    fun debug(message: Any) {
        Bot.LOG.debug("[${name.toUpperCase()}] $message")
    }

    override fun toString(): String {
        return "Module(name='$name', loaded=$loaded)"
    }

    fun getProp(string: String, default: String? = null): String? = Bot.properties.getProperty(
            string) ?: default

    fun triggerPeriodicTasks(count: Int) {
        if(count > 0) {
            this.periodicTasks.forEach { method, interval ->
                if ((count % interval) == 0) {
                    try {
                        method.invoke(this@Module)
                    } catch(e: InvocationTargetException){
                        log("An error occurred when executing ${method.name}: ${e.targetException}")
                        e.targetException.printStackTrace()
                    } catch(e: Throwable){
                        log("An error occurred when executing ${method.name}: $e")
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    protected annotation class Periodic(val interval: Int)
}