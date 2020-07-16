package com.mrkirby153.kirbot.services.command.context

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import kotlin.reflect.KClass

/**
 * The context of a command passed to each resolver
 */
class CommandContext(private val args: MutableList<String>,
                     /**
                      * The parameter that is currently being resolved
                      */
                     val parameter: CommandParameter,
                     /**
                      * The issuer of the command
                      */
                     val issuer: User,
                     /**
                      * The guild (if any) that this command was executed on
                      */
                     val guild: Guild?) {


    /**
     * Gets and removes the first element from the command arguments
     *
     * @return The argument or null if it does not exist
     */
    fun popFirst(): String? {
        if (args.isEmpty())
            return null
        return args.removeAt(0)
    }

    /**
     * Checks if there are remaining arguments to consume
     *
     * @return True if there are arguments remaining to consume
     */
    fun hasNext() = args.isNotEmpty()

    /**
     * Gets the first argument without consuming it
     *
     * @return The first argument or null if there is none
     */
    fun getFirst() = if (args.isEmpty()) null else args[0]

    fun <T : Annotation> getAnnotation(clazz: KClass<T>) : T = parameter.parameter.getAnnotation(clazz.java)

    fun hasAnnotation(clazz: KClass<out Annotation>) = parameter.parameter.isAnnotationPresent(clazz.java)

}