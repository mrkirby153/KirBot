package me.mrkirby153.KirBot.utils.embed

import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.requests.RestAction
import java.awt.Color

val info_color = Color(0, 80, 175)
val error_color = Color.RED

class ResponseBuilder(val context: Context) {
    /**
     * Quick-reply to a message.
     *
     * @param text The text to send.
     * @return The Message created by this function.
     */
    fun text(text: String): RestAction<Message> {
//        return if (context.context.canTalk()) {
        return context.channel.sendMessage(text)
//        } else {
//        context.user.openPrivateChannel().complete().sendMessage(text)
//        }
    }

    /**
     * Send a standard info message.
     *
     * @param msg The text to send.
     * @return The Message created by this function.
     */
    fun info(msg: String): RestAction<Message> {
        return embed {
            setTitle("Info")
            setDescription(msg)
            setColor(info_color)
        }.rest()
    }

    /**
     * Send a standard success message
     *
     * @param msg The text to send
     * @return The message created by this function
     */
    fun success(msg: String): RestAction<Message> {
        return embed {
            setTitle("Success")
            setDescription(msg)
            setColor(Color.GREEN)
        }.rest()
    }

    /**
     * Send a standard error message.
     *
     * @param msg The text to send.
     * @return The Message created by this function.
     */
    fun error(msg: String): RestAction<Message> {
        return embed {
            setTitle("Error")
            setDescription(msg)
            setColor(error_color)
        }.rest()
    }

    /**
     * Send a standard exception message.
     *
     * @return The Message created by this function.
     */
    fun exception(exception: Exception): RestAction<Message> {
        return embed {
            setTitle("Exception")
            setDescription(exception.message)
            setColor(error_color)
        }.rest()
    }

    /**
     * Creates an EmbedBuilder to be used to creates an embed to send.
     * <br> This builder can use [ResponseEmbedBuilder.rest] to quickly send the built embed.
     *
     * @param title Title of the embed.
     */
    @JvmOverloads
    fun embed(title: String? = null): ResponseEmbedBuilder = ResponseEmbedBuilder().apply {
        setTitle(title)
        setColor(info_color)
    }

    /**
     * Creates an EmbedBuilder to be used to creates an embed to send.
     * <br> This builder can use [ResponseEmbedBuilder.rest] to quickly send the built embed.
     *
     * @param title Title of the embed.
     */
    inline fun embed(title: String? = null, value: ResponseEmbedBuilder.() -> Unit): ResponseEmbedBuilder {
        return embed(title).apply(value)
    }

    @Suppress("NOTHING_TO_INLINE")
    inner class ResponseEmbedBuilder : EmbedProxy<ResponseEmbedBuilder>() {
        fun rest(): RestAction<Message> {
            return context.channel.sendMessage(build())
        }
    }
}