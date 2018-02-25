package me.mrkirby153.KirBot.module

import me.mrkirby153.KirBot.Bot

abstract class Module(val name: String) {

    var loaded = false

    val dependencies = mutableListOf<Class<*>>()


    abstract fun onLoad()

    open fun onUnload() {}

    fun load() {
        log("Starting load")
        val unmetDeps = this.getUnmetDeps()
        if (unmetDeps.isNotEmpty()) {
            throw IllegalStateException(
                    "This module has unloaded dependencies, it will not be loaded ($unmetDeps)")
        }
        debug("Calling onLoad()")
        onLoad()
        debug("Registering listener")
        Bot.shardManager.addListener(this)
        debug("Load complete")
        loaded = true
    }

    fun unload() {
        log("Starting unload")
        if (!loaded) {
            throw IllegalStateException(
                    "Attempting to unload a module that has already been unloaded")
        }
        debug("Calling onUnload()")
        onUnload()
        debug("Removing listener")
        Bot.shardManager.removeListener(this)
        log("Unloading complete")
        loaded = false
    }

    fun getUnmetDeps(): List<Class<*>> {
        return dependencies.filter { it !in ModuleManager.loadedModules.map { it.javaClass } }
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


}