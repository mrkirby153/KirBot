package me.mrkirby153.KirBot.utils.embed

import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.GREEN_TICK
import me.mrkirby153.KirBot.utils.RED_TICK
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.requests.RestAction
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
        return context.channel.sendMessage(text)
    }

    /**
     * Send a standard info message.
     *
     * @param msg The text to send.
     * @return The Message created by this function.
     */
    fun info(msg: String): RestAction<Message> {
        return text(msg)
    }

    /**
     * Send a standard success message
     *
     * @param msg The text to send
     * @return The message created by this function
     */
    fun success(msg: String, hand: Boolean = false): RestAction<Message> {
        return if (hand) {
            this.context.channel.sendMessage(":ok_hand: $msg")
        } else {
            this.context.channel.sendMessage("$GREEN_TICK $msg")
        }
    }

    /**
     * Send a standard error message.
     *
     * @param msg The text to send.
     * @return The Message created by this function.
     */
    fun error(msg: String): RestAction<Message> {
        return this.context.channel.sendMessage("$RED_TICK $msg")
    }

    /**
     * Send a standard exception message.
     *
     * @return The Message created by this function.
     */
    fun exception(exception: Exception): RestAction<Message> {
        return error("An exception occurred: ${exception.message}")
    }

    /**
     * Creates an EmbedBuilder to be used to creates an embed to send.
     * <br> This builder can use [ResponseEmbedBuilder.rest] to quickly send the built embed.
     *
     * @param title Title of the embed.
     */
    @JvmOverloads
    fun embed(title: String? = null): ResponseEmbedBuilder = ResponseEmbedBuilder().apply {
        if (title != null)
            title { +title }
        color = info_color
    }

    /**
     * Creates an EmbedBuilder to be used to creates an embed to send.
     * <br> This builder can use [ResponseEmbedBuilder.rest] to quickly send the built embed.
     *
     * @param title Title of the embed.
     */
    inline fun embed(title: String? = null,
                     value: ResponseEmbedBuilder.() -> Unit): ResponseEmbedBuilder {
        return embed(title).apply(value)
    }

    inner class ResponseEmbedBuilder : EmbedBuilder() {
        fun rest(): RestAction<Message> {
            return context.channel.sendMessage(build())
        }
    }
}