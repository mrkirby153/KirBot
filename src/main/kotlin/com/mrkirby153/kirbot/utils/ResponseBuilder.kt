package com.mrkirby153.kirbot.utils

import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.IMentionable
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.utils.MarkdownSanitizer
import org.apache.logging.log4j.LogManager

/**
 * A builder for sanitizing and handling responses to messages
 */
class ResponseBuilder(private val channel: MessageChannel) {

    private val log = LogManager.getLogger()

    /**
     * A list of mention types that will always be allowed
     */
    val alwaysMentionable = listOf(Message.MentionType.EMOTE, Message.MentionType.CHANNEL)


    /**
     * Sends a message to the channel. By default, only emotes and channels can be mentioned. All
     * other mention types must be explicitly stated
     *
     * @param message The message to send to the channel. Markdown surrounded with {{ and }} will be
     * escaped
     * @param allowMentions Any additional mention types to allow in addition to [alwaysMentionable]
     * @param mentionRoles A list of role ids to enable mentioning
     * @param mentionUsers A list of user ids to enable mentioning
     * @return A rest action for sending the message
     */
    fun sendMessage(message: String, allowMentions: List<Message.MentionType> = emptyList(),
                    mentionRoles: List<String> = emptyList(),
                    mentionUsers: List<String> = emptyList()): RestAction<Message>? {
        if(!channel.checkPermissions(Permission.MESSAGE_WRITE)) {
            log.debug("No permission to send messages in $channel. Silently failing")
            return null
        }
        val mentions = mutableSetOf<Message.MentionType>()
        alwaysMentionable.toCollection(mentions)
        allowMentions.toCollection(mentions)

        if (mentionRoles.isNotEmpty())
            mentions.add(Message.MentionType.ROLE)
        if (mentionUsers.isNotEmpty())
            mentions.add(Message.MentionType.USER)

        val builder = MessageBuilder(escapeMarkdown(message))
        builder.setAllowedMentions(mentions)
        builder.mentionRoles(*mentionRoles.toTypedArray())
        builder.mentionUsers(*mentionUsers.toTypedArray())
        val msg = builder.build()
        return channel.sendMessage(msg)
    }

    /**
     * Sends a message to the channel. By default only emotes and channels will be mentioned.
     *
     * @param message The message to send to the channel. Markdown surrounded with {{ and }} will be
     * escaped
     * @param allowMentions The types of mentions to allow in addition to [alwaysMentionable]
     * @param mentionable A list of mentionable entities to mention
     * @return A rest action for sending the message
     */
    fun sendMessage(message: String, allowMentions: List<Message.MentionType> = emptyList(),
                    mentionable: List<IMentionable> = emptyList()): RestAction<Message>? {
        val users = mutableListOf<String>()
        val roles = mutableListOf<String>()
        mentionable.forEach {
            when (mentionable) {
                is Member, is User -> users.add(it.id)
                is Role -> roles.add(it.id)
            }
        }
        return sendMessage(message, allowMentions, users, roles)
    }

    companion object {

        private val escapePattern = Regex("(?<!\\\\)\\{\\{(.*)}}")

        /**
         * Escapes all markdown unless surrounded with `{{ }}`
         *
         * @param message The message to escape
         * @return The sanitized markdown
         */
        fun escapeMarkdown(message: String): String {
            return buildString {
                var start = 0
                escapePattern.findAll(message).forEach { pattern ->
                    append(MarkdownSanitizer.sanitize(
                            message.subSequence(start until pattern.range.first) as String, MarkdownSanitizer.SanitizationStrategy.ESCAPE))
                    val text = pattern.groups[1] ?: return@forEach
                    append(text.value)
                    start = pattern.range.last + 1
                }
                append(MarkdownSanitizer.sanitize(message.substring(start), MarkdownSanitizer.SanitizationStrategy.ESCAPE))
            }
        }
    }

}