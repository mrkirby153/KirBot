package me.mrkirby153.KirBot.server

import me.mrkirby153.KirBot.command.CommandManager
import me.mrkirby153.KirBot.music.MusicManager
import me.mrkirby153.KirBot.server.data.DataRepository
import me.mrkirby153.KirBot.utils.child
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import net.dv8tion.jda.core.managers.GuildManager
import java.nio.charset.Charset
import java.util.regex.Pattern

/**
 * Represent the bot
 */
class Server(guild: Guild) : GuildManager(guild), Guild by guild {

    private var repository: DataRepository? = null

    val musicManager = MusicManager(this)

    val logger = ServerLogger(this)

    fun handleMessageEvent(event: MessageReceivedEvent) {
        // Ignore PMs
        if (event.isFromType(ChannelType.PRIVATE))
            return
        // Call message processors
        var rawMsgText = event.message.content
        // TODO 5/4/2017 Extract to own method
        for (processor in CommandManager.messageProcessors) {
            val proc = processor.newInstance()
            proc.matches = emptyArray()
            proc.server = this
            // Compile regex
            val pattern = Pattern.compile("(?<=${escape(proc.startSequence)})(.*?)(?=${escape(proc.endSequence)})")

            val matches = mutableListOf<String>()
            loop@ while (true) {
                val matcher = pattern.matcher(rawMsgText)

                if (matcher.find()) {
                    val part = rawMsgText.substring(matcher.start(), matcher.end())
                    matches.add(part)
                    rawMsgText = rawMsgText.substring(startIndex = Math.min(matcher.end() + proc.endSequence.length, rawMsgText.length))
                    if (rawMsgText.isEmpty())
                        break@loop
                } else {
                    break@loop
                }
            }
            proc.matches = matches.toTypedArray()
            if (proc.matches.isNotEmpty())
                proc.process(event.message)
            if (proc.stopProcessing)
                break
        }
        CommandManager.call(event)
    }

    private fun escape(string: String): String {
        return buildString {
            string.forEach {
                this@buildString.append("\\$it")
            }
        }
    }

    fun kick(user: String): Boolean {
        try {
            guild.controller.kick(user).queue()
            return true
        } catch (e: PermissionException) {
            return false
        }
    }

    fun ban(user: String): Boolean {
        try {
            guild.controller.ban(user, 2).queue()
            return true
        } catch (e: PermissionException) {
            return false
        }
    }

    fun deleteMessage(msg: Message): Boolean {
        try {
            msg.delete().queue()
            return true
        } catch (e: PermissionException) {
            return false
        }
    }

    @JvmOverloads
    fun repository(forceUpdate: Boolean = false): DataRepository? {
        if (repository == null || forceUpdate) {
            val fileName = "$id.json"
            val file = ServerRepository.serverDirectory.child(fileName)
            if (!file.exists()) {
                DataRepository(this).save()
                return repository()
            }
            val reader = file.reader(Charset.defaultCharset())
            val server = ServerRepository.gson.fromJson(reader, DataRepository::class.java)
            server.server = this
            this.repository = server
            reader.close()
            return server
        } else {
            return repository
        }
    }

}