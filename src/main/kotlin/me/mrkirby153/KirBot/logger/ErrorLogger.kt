package me.mrkirby153.KirBot.logger

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.modules.AdminControl
import me.mrkirby153.KirBot.utils.deleteAfter
import me.mrkirby153.KirBot.utils.embed.b
import me.mrkirby153.kcutils.child
import me.mrkirby153.kcutils.utils.IdGenerator
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.concurrent.TimeUnit


object ErrorLogger {

    private val chanId = Bot.properties.getProperty("error-channel", "0")
    private val file = Bot.files.data.child("errors.json")
    private var errorRepository = mutableMapOf<String, ReportedError>()
    private val idGenerator = IdGenerator(IdGenerator.ALPHA)

    private val gson = Gson()

    private val channel: TextChannel? by lazy {
         AdminControl.logChannel
    }

    init {
        load()
    }

    fun save() {
        val json = gson.toJson(errorRepository)
        file.writer().use {
            it.write(json)
            it.flush()
        }
    }

    fun load() {
        val typeToken = object : TypeToken<Map<String, ReportedError>>() {}.type
        file.reader().use {
            errorRepository = gson.fromJson(it, typeToken)
        }
    }

    fun logThrowable(throwable: Throwable, guild: Guild? = null, user: User? = null): String? {
        if (channel == null)
            return null
        val dupe = findDupe(throwable)
        if (dupe != null) {
            Bot.LOG.debug("Found duplicate")
            dupe.occurrences++
            val shouldEdit = System.currentTimeMillis() - dupe.lastSeen > 30 * 1000 // Only edit the message once every 30 seconds
            dupe.lastSeen = System.currentTimeMillis()
            errorRepository[dupe.id] = dupe
            save()
            if (shouldEdit)
                channel?.getMessageById(dupe.messageId)?.queue {
                    it.editMessage(buildReport(throwable, occurrences = dupe.occurrences,
                            id = dupe.id)).queue()
                }
            return dupe.id
        }
        val id = buildString {
            var i: String
            do {
                i = idGenerator.generate(10)
            } while (i in errorRepository.keys)
            append(i)
        }
        throwable.printStackTrace()
        Bot.LOG.error("Logged error $id!")
        channel?.sendMessage(buildReport(throwable, guild, user, id))?.queue {
            errorRepository[id] = ReportedError(id, throwable.javaClass.canonicalName,
                    throwable.message,
                    buildStacktrace(throwable),
                    it.id, 1, System.currentTimeMillis())
            save()
        }
        return id
    }

    private fun buildReport(throwable: Throwable, guild: Guild? = null,
                            user: User? = null, id: String, occurrences: Int = 1): String {
        return buildString {
            append("───────────────────\n")
            append(b("Unhandled Exception"))
            append("\n")
            append(b("Exception: "))
            append(throwable.javaClass.canonicalName)
            append(" (${throwable.message})")
            append("\n")
            if (guild != null) {
                append("\n")
                append(b("Guild: ") + "${guild.name} (`${guild.id}`)")
            }
            if (user != null) {
                append("\n")
                append(b("User: ") + "${user.name} (`${user.id}`)")
            }
            append("\n" + b("Thread: ") + Thread.currentThread().name)
            if (occurrences > 1) {
                append("\n" + b("Seen: ") + occurrences + " times")
            }
            append("\n" + b("ID: ") + id)
        }
    }

    fun getTrace(id: String): String? = errorRepository[id]?.stacktrace

    fun acknowledge(id: String) {
        val error = errorRepository.remove(id) ?: return
        channel?.getMessageById(error.messageId)?.queue({
            it.delete().queue()
        }, {
            Bot.LOG.warn("Message $error was already deleted")
        })
        channel?.sendMessage("Acknowledged `$id`")?.queue {
            it.deleteAfter(10, TimeUnit.SECONDS)
        }
    }

    fun ackAll() {
        if(errorRepository.values.isEmpty())
            return
        val messageIds = errorRepository.values.map { it.messageId }.toMutableList()
        channel?.purgeMessagesById(messageIds)
        errorRepository.clear()
        save()
    }

    fun findDupe(throwable: Throwable): ReportedError? {
        return errorRepository.values.firstOrNull {
            it.exception.equals(throwable.javaClass.canonicalName, true) && it.msg.equals(
                    throwable.message) && buildStacktrace(throwable).equals(it.stacktrace, true)
        }
    }

    private fun buildStacktrace(throwable: Throwable): String {
        val outputStream = ByteArrayOutputStream()
        val ps = PrintStream(outputStream)
        throwable.printStackTrace(ps)
        return outputStream.toString()
    }

    data class ReportedError(val id: String, val exception: String, val msg: String?,
                             val stacktrace: String,
                             val messageId: String, var occurrences: Int, var lastSeen: Long)
}