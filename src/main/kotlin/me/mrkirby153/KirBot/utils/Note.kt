package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.server.Server
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.exceptions.PermissionException
import java.awt.Color
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class Note(val server: Server, private val message: Message) : Message by message {

    fun info(msg: String): Future<Note> {
        val eb = EmbedBuilder().setAuthor("Info", null, null).setDescription(msg).setColor(Color.BLUE)
        return channel.sendMessage(eb.build()).submit().toNote()
    }

    fun error(msg: String): Future<Note> {
        val eb = EmbedBuilder().setAuthor("Error", null, null).setDescription(msg).setColor(Color.RED)
        return channel.sendMessage(eb.build()).submit().toNote()
    }

    fun delete() : Boolean {
        try{
            deleteMessage().queue()
            return true
        } catch (e : PermissionException){
            return false
        }
    }

    fun edit(msg: String) = editMessage(msg).submit().toNote()

    @JvmOverloads
    fun replyEmbed(title: String?, msg: String?,
                   color: Color? = Color.WHITE, thumb:
                    String? = null, img : String? = null): Future<Note> = channel.sendMessage(makeEmbed(title, msg, color, img, thumb, author)).submit().toNote()

    private fun Future<Message>.toNote(): Future<Note> = object : CompletableFuture<Note>() {
        override fun get(): Note = Note(server, this@toNote.get())
    }
}