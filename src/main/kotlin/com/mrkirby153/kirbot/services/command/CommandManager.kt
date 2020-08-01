package com.mrkirby153.kirbot.services.command

import com.mrkirby153.kirbot.entity.repo.CommandAliasRepository
import com.mrkirby153.kirbot.events.CommandExecutedEvent
import com.mrkirby153.kirbot.services.PermissionService
import com.mrkirby153.kirbot.services.command.context.ArgumentParseException
import com.mrkirby153.kirbot.services.command.context.CommandContextResolver
import com.mrkirby153.kirbot.services.command.context.ContextResolvers
import com.mrkirby153.kirbot.services.command.context.Optional
import com.mrkirby153.kirbot.services.command.context.Parameter
import com.mrkirby153.kirbot.services.setting.GuildSettings
import com.mrkirby153.kirbot.services.setting.SettingsService
import com.mrkirby153.kirbot.utils.RED_TICK
import com.mrkirby153.kirbot.utils.checkPermissions
import com.mrkirby153.kirbot.utils.getMember
import com.mrkirby153.kirbot.utils.queue
import com.mrkirby153.kirbot.utils.responseBuilder
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.sharding.ShardManager
import org.apache.logging.log4j.LogManager
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.event.EventListener
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.util.ClassUtils
import java.lang.reflect.InvocationTargetException
import java.util.LinkedList
import javax.annotation.PostConstruct
import kotlin.math.max
import kotlin.system.measureTimeMillis

@Service
class CommandManager(private val context: ApplicationContext,
                     private val commandContextResolver: CommandContextResolver,
                     private val contextResolver: ContextResolvers,
                     private val settingsService: SettingsService,
                     private val shardManager: ShardManager,
                     private val permissionService: PermissionService,
                     private val eventPublisher: ApplicationEventPublisher,
                     private val commandAliasRepository: CommandAliasRepository) : CommandService {

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

    override fun executeCommand(message: String, user: User, channel: MessageChannel) {
        if (channel.type == ChannelType.PRIVATE)
            return
        channel as TextChannel

        val prefix = settingsService.getSetting(GuildSettings.commandPrefix, channel.guild)!!
        val mention = isMention(message)
        val member = user.getMember(channel.guild) ?: return

        if (!message.startsWith(prefix) && !mention)
            return

        log.debug("Starting processing command \"$message\". Mention? $mention")
        val time = measureTimeMillis a@{
            val commandArgs = LinkedList(stripPrefix(message, prefix).split(" "))

            if (commandArgs.isEmpty()) {
                if (mention) {
                    channel.responseBuilder.sendMessage(
                            "The command prefix on this server is {{`$prefix`}}").queue()
                    return@a
                }
            }

            var aliasClearance: Long? = null
            commandAliasRepository.getCommandAliasByCommandIgnoringCaseAndServerId(commandArgs[0], channel.guild.id).ifPresent { alias ->
                log.debug("Overriding command \"{}\" with aliased command \"{}\"", commandArgs[0], alias.alias)
                if(alias.alias == null) {
                    return@ifPresent
                }
                commandArgs.removeAt(0)
                commandArgs.add(0, alias.alias)
                aliasClearance = alias.clearance
            }
            val node = resolve(commandArgs) ?: return@a

            val defaultClearance = commandAliasRepository.getDefaultClearance(channel.guild.id).orElse(0L)
            val cmdClearance = max(defaultClearance, aliasClearance ?: node.annotation.clearance)
            val userClearance = permissionService.getClearance(user, channel.guild)

            fun notifyNoPermission() {
                val shouldNotify = settingsService.getSetting(GuildSettings.commandSilentFail,
                        channel.guild) ?: false
                if (shouldNotify)
                    channel.responseBuilder.sendMessage(
                            ":lock: You do not have permission to perform this command").queue()
            }

            if(userClearance < cmdClearance) {
                val requiredPerms = node.annotation.userPermissions
                if(requiredPerms.isEmpty()) {
                    notifyNoPermission()
                    return@a
                }
                val missing = requiredPerms.filter { !member.hasPermission(it) }
                if (missing.isNotEmpty()) {
                    notifyNoPermission()
                    return@a
                }
            }

            val missingPerms = node.annotation.permissions.filter { !channel.checkPermissions(it) }
            if (missingPerms.isNotEmpty()) {
                channel.responseBuilder.sendMessage(
                        "$RED_TICK Missing the following permissions: {{`${missingPerms.joinToString(
                                ", ")}`}}").queue()
                return@a
            }
            try {
                eventPublisher.publishEvent(
                        CommandExecutedEvent(node, commandArgs.toList(), user, channel.guild))
                invoke(node, commandArgs, user, channel.guild, channel)
            } catch (e: CommandException) {
                channel.responseBuilder.sendMessage("$RED_TICK " +
                       (e.message ?: "An unknown error occurred!")).queue()
            } catch (e: ArgumentParseException) {
                val msg = buildString {
                    append("$RED_TICK")
                    if(e.message != null) {
                        appendln(e.message)
                    } else {
                        appendln("An unknown error occurred parsing arguments!")
                    }
                    append("Usage: {{`$prefix${node.fullName} ${getUsageString(node)}`}}")
                }
                channel.responseBuilder.sendMessage(msg).queue()
            } catch (e: Exception) {
                log.error("An unknown error occurred when executing", e)
                channel.responseBuilder.sendMessage("$RED_TICK An unknown error occurred!").queue()
            }
        }
        log.debug("Finished processing command \"$message\" in $time ms")
    }

    @EventListener
    @Async
    fun onMessage(event: GuildMessageReceivedEvent) {
        if (event.isWebhookMessage || event.author.isBot)
            return
        executeCommand(event.message.contentRaw, event.author, event.channel)
    }

    override fun invoke(node: CommandNode, args: List<String>, user: User, guild: Guild?, channel: TextChannel) {
        if (node.isSkeleton())
            return
        val parameterValues = commandContextResolver.resolve(node, args, user, guild, channel)
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

    private fun isMention(message: String) = message.matches(
            Regex("^<@!?${shardManager.shards[0].selfUser.id}>.*"))

    private fun stripPrefix(message: String, prefix: String) = if (isMention(
                    message)) message.replace(Regex("^<@!?\\d{17,18}>\\s?"),
            "") else message.substring(prefix.length)

    private fun resolve(args: LinkedList<String>): CommandNode? {
        var current = commandTree
        while (args.isNotEmpty()) {
            val found = current.getChild(args.peek())
            if (found == null) {
                break
            } else {
                current = found
                args.pop()
            }
        }
        return if (current == commandTree) null else current
    }
}