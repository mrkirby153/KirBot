package com.mrkirby153.kirbot.services.command.context

import com.mrkirby153.kirbot.services.command.CommandNode
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.springframework.stereotype.Component

@Component
class CommandContextResolver(private val contextResolvers: ContextResolvers) {

    /**
     * Resolve the arguments into a list of parameters for method invocation
     *
     * @param node The command node
     * @param args The arguments to resolve
     * @param issuer The issuer of the command
     * @param guild The guild that the command is being run on
     * @return A list of method parameters
     */
    fun resolve(node: CommandNode, args: List<String>, issuer: User, guild: Guild?): List<Any?> {
        val params = mutableListOf<Any?>()
        val remainingArgs = args.toMutableList()
        val method = node.method!!
        var processingOptional = false
        method.parameters.forEachIndexed { index, param ->
            val name = param.getAnnotation(Parameter::class.java)?.value ?: param.name
            val optional = param.isAnnotationPresent(Optional::class.java)
            val resolver = contextResolvers.get(param.type) ?: throw MissingResolverException(
                    param.type)
            if (!optional && processingOptional)
                throw ArgumentParseException(
                        "Cannot parse non-optional arguments after optional arguments")
            if (optional)
                processingOptional = true
            if (remainingArgs.isEmpty()) {
                if (resolver.consuming) {
                    if (!optional) {
                        throw MissingArgumentException(name)
                    } else {
                        params.add(null)
                        return@forEachIndexed
                    }
                }
            }
            params.add(resolver.resolver.invoke(CommandContext(remainingArgs,
                    CommandParameter(param.type, param, param.name,
                            method.parameterCount, index), issuer, guild)))
        }
        return params.toList()
    }
}