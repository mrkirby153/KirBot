package com.mrkirby153.kirbot.services.command.context

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.sharding.ShardManager
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Component
import java.util.regex.Pattern
import kotlin.reflect.KClass

@Component
class ContextResolvers(val shardManager: ShardManager) {

    private val log = LogManager.getLogger()

    private val snowflakePattern = Pattern.compile("\\d{17,18}")

    init {
        register(Int::class) {
            val num = it.popFirst() ?: return@register null
            val min = it.getAnnotation(Min::class)
            val max = it.getAnnotation(Max::class)
            convertAndCheckBounds(num, min.value, max.value).toInt()
        }
        register(Long::class) {
            val num = it.popFirst() ?: return@register null
            val min = it.getAnnotation(Min::class)
            val max = it.getAnnotation(Max::class)
            convertAndCheckBounds(num, min.value, max.value)
        }
        register(Boolean::class) {
            val str = it.popFirst() ?: return@register null
            return@register when (str.toLowerCase()) {
                "true" -> true
                "false" -> false
                else -> throw ArgumentParseException("Invalid boolean \"$str\"")
            }
        }
        register(CommandSender::class) {
            CommandSender(it.issuer)
        }
        register(CurrentGuild::class) {
            if (it.guild != null)
                CurrentGuild(it.guild)
            else
                null
        }
        register(String::class) {
            if (!it.hasNext())
                return@register null
            if (it.hasAnnotation(Single::class))
                return@register it.popFirst()
            return@register buildString {
                var str: String? = it.popFirst()
                do {
                    append("$str ")
                    str = it.popFirst()
                } while (str != null)
            }.trim()
        }
        register(User::class) {
            val id = it.popFirst() ?: return@register null
            val matcher = snowflakePattern.matcher(id)
            if (matcher.find()) {
                shardManager.getUserById(matcher.group()) ?: throw ArgumentParseException(
                        "User with the id of \"$id\" was not found")
            } else {
                throw ArgumentParseException("Could not extract a valid user id out of \"$id\"")
            }
        }
        register(Member::class) {
            val id = it.popFirst() ?: return@register null
            val matcher = snowflakePattern.matcher(id)
            if (matcher.find()) {
                it.guild?.getMemberById(id) ?: throw ArgumentParseException(
                        "User with the id of \"$id\" was not found")
            } else {
                throw ArgumentParseException("Could not extract a valid user id out of \"$id\"")
            }
        }
        register(MessageChannel::class) {
            val id = it.popFirst() ?: return@register null
            val matcher = snowflakePattern.matcher(id)
            if (matcher.find()) {
                it.guild?.getTextChannelById(id) ?: throw ArgumentParseException(
                        "Channel \"$id\" was not found")
            } else {
                throw ArgumentParseException("No channel id could be extracted from \"$id\"")
            }
        }
        register(VoiceChannel::class) {
            val id = it.popFirst() ?: return@register null
            val matcher = snowflakePattern.matcher(id)
            if (matcher.find()) {
                it.guild?.getVoiceChannelById(id) ?: throw ArgumentParseException(
                        "Channel \"$id\" was not found")
            } else {
                throw ArgumentParseException("No channel id could be extracted from \"$id\"")
            }
        }
        register(Guild::class) {
            val id = it.popFirst() ?: return@register null
            val matcher = snowflakePattern.matcher(id)
            if (matcher.find()) {
                shardManager.getGuildById(id) ?: throw ArgumentParseException(
                        "Guild \"$id\" was not found")
            } else {
                throw ArgumentParseException("No guild id could be extracted from \"$id\"")
            }
        }
    }

    private val contexts = mutableMapOf<Class<*>, (CommandContext) -> Any?>()


    final fun register(clazz: KClass<*>, function: (CommandContext) -> Any?) {
        if (contexts.containsKey(clazz.java))
            throw IllegalArgumentException(
                    "Attempting to register a resolver for $clazz which already exists")
        contexts[clazz.java] = function
        log.info("Registered resolver for {}", clazz)
    }

    fun get(clazz: Class<*>) = contexts.get(clazz)

    private fun convertAndCheckBounds(str: String, min: Long, max: Long): Long {
        try {
            val num = str.toLong()
            if (num < min)
                throw ArgumentParseException("Must be greater than $min")
            if (num > max)
                throw ArgumentParseException("Must be less than $max")
            return num
        } catch (e: NumberFormatException) {
            throw ArgumentParseException("\$str\" is not a valid number")
        }
    }
}