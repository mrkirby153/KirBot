package com.mrkirby153.kirbot.services.command

import com.mrkirby153.kirbot.services.command.context.CommandContextResolver
import com.mrkirby153.kirbot.services.command.context.ContextResolvers
import com.mrkirby153.kirbot.services.command.context.Optional
import com.mrkirby153.kirbot.services.command.context.Parameter
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Service
import org.springframework.util.ClassUtils
import java.lang.reflect.InvocationTargetException
import javax.annotation.PostConstruct
import kotlin.system.measureTimeMillis

@Service
class CommandManager(private val context: ApplicationContext,
                     private val commandContextResolver: CommandContextResolver,
                     private val contextResolver: ContextResolvers) : CommandService {

    private val automaticDiscoveryPackage = "com.mrkirby153.kirbot"

    private val log = LogManager.getLogger()

    private val commandTree: CommandNode = CommandNode("")


    override fun registerCommand(clazz: Any) {
        val realClass = ClassUtils.getUserClass(clazz)
        log.info("Registering commands in $realClass")

        realClass.methods.filter { it.isAnnotationPresent(Command::class.java) }.forEach { method ->
            val annotation = method.getAnnotation(Command::class.java)

            val name = annotation.name.split(" ")
            val node = CommandNode(name.last(), method, realClass, clazz)
            insertCommandNode(name.dropLast(1), node)
        }
    }

    override fun getCommand(args: String) = getCommand(args.split(" "))

    override fun getCommand(args: List<String>): CommandNode? {
        var curr = commandTree
        args.forEach {
            curr = curr.getChild(it) ?: return null
        }
        return curr
    }

    override fun executeCommand(args: String) {
        TODO("Not yet implemented")
    }

    override fun invoke(node: CommandNode, args: List<String>, user: User, guild: Guild?) {
        if (node.isSkeleton())
            return
        val parameterValues = commandContextResolver.resolve(node, args, user, guild)
        try {
            node.method!!.invoke(node.instance, *parameterValues.toTypedArray())
        } catch (e: InvocationTargetException) {
            val cause = e.cause ?: e
            throw CommandException(cause.message ?: "An unknown error occurred")
        }
    }

    override fun getUsageString(node: CommandNode): String {
        if (node.isSkeleton())
            throw IllegalArgumentException("Can't get usage string for a skeleton command node")
        return node.method!!.parameters.filter { contextResolver.consumes(it.type) }.joinToString(
                " ") {
            val optional = it.isAnnotationPresent(Optional::class.java)
            val name = it.getAnnotation(Parameter::class.java)?.value ?: it.name
            if (optional) {
                "[$name]"
            } else {
                "<$name>"
            }
        }
    }

    @PostConstruct
    fun discoverCommands() {
        log.info("Starting discovery of commands in $automaticDiscoveryPackage")
        var count = 0
        val time = measureTimeMillis {
            val provider = ClassPathScanningCandidateComponentProvider(false)
            provider.addIncludeFilter(AnnotationTypeFilter(Commands::class.java))

            provider.findCandidateComponents(automaticDiscoveryPackage).forEach { def ->
                val clazz = Class.forName(def.beanClassName)
                log.debug("Discovered class {}", clazz)
                val instance = try {
                    context.getBean(clazz)
                } catch (e: NoSuchBeanDefinitionException) {
                    log.debug("{} is not a bean. Attempting to instance with default constructor",
                            clazz)
                    try {
                        clazz.getConstructor().newInstance()
                    } catch (e: NoSuchMethodError) {
                        log.error("Could not instance {}. Missing default constructor", clazz)
                        return@forEach
                    } catch (e: InstantiationError) {
                        log.error("Could not instance {}", clazz, e)
                        return@forEach
                    }
                }
                log.debug("Instanced {} as {}", clazz, instance)
                try {
                    registerCommand(instance)
                } catch (e: IllegalArgumentException) {
                    log.error("Could not automatically register {}", clazz, e)
                }
                count++
            }
        }
        log.info("Registered {} classes in {}", count, Time.format(1, time))
    }

    /**
     * Inserts a command node into the command tree
     *
     * @param path The path to insert the command in
     * @param node The node to insert into the tree
     * @return The command node of the newly inserted node. The provided node should not be relied
     * on as it may change and no longer be valid after insertion
     */
    private fun insertCommandNode(path: List<String>, node: CommandNode): CommandNode {
        var curr = commandTree
        path.forEach {
            val found = node.getChild(it)
            curr = if (found != null) {
                found
            } else {
                val new = CommandNode(it)
                curr.addChild(new)
                new
            }
        }
        // We should now be 1 above the expected insertion point
        val existing = curr.getChild(node.name)
        return if (existing != null) {
            if (existing.isSkeleton()) {
                // Promote the skeleton node to a main node
                existing.clazz = node.clazz
                existing.method = node.method
                existing
            } else {
                throw IllegalArgumentException(
                        "Attempting to register a command that already exists")
            }
        } else {
            curr.addChild(node)
            node
        }
    }
}