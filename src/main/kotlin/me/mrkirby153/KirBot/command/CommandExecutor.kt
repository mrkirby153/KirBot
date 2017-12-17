package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.user.Clearance
import org.reflections.Reflections

object CommandExecutor {

    val commands = mutableListOf<CommandSpec>()


    fun loadAll() {
        val reflections = Reflections("me.mrkirby153.KirBot")

        val commands = reflections.getSubTypesOf(BaseCommand::class.java)

        Bot.LOG.debug("Found ${commands.size} commands")

        commands.forEach(CommandExecutor::registerCommand)
    }

    fun execute(){

    }

    fun registerCommand(clazz: Class<*>) {
        Bot.LOG.debug("Registering command ${clazz.canonicalName}")
        try {
            val instance = clazz.newInstance() as? BaseCommand ?: return

            val spec = instance.getSpec()

            val cmdAnnotation = clazz.getAnnotation(Command::class.java)
            spec.aliases.addAll(cmdAnnotation.value.split(","))
            spec.executor = instance

            val clearanceAnnotation = clazz.getAnnotation(RequiresClearance::class.java)
            spec.clearance = clearanceAnnotation?.value ?: Clearance.USER

            commands.add(spec)
        } catch (e: Exception) {
            e.printStackTrace()
            Bot.LOG.error("An error occurred when registering ${clazz.canonicalName}")
        }
    }
}