package com.mrkirby153.kirbot.entity.guild.repo

import com.mrkirby153.kirbot.entity.guild.LoggedMessage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface LoggedMessageRepository : JpaRepository<LoggedMessage, String> {

    fun getAllByAuthorAndServerId(authorId: String, guild: String): List<LoggedMessage>

    fun getAllByChannelAndServerId(channelId: String, guild: String): List<LoggedMessage>

    fun deleteAllByServerId(guild: String): Boolean

    fun deleteAllByChannel(channelId: String): Boolean

    @Query("UPDATE LoggedMessage SET message = (:content), editCount = editCount + 1, updatedAt = current_timestamp WHERE id = (:messageId)")
    fun updateMessage(messageId: String, content: String)

    @Query("UPDATE LoggedMessage SET deleted = true WHERE id = (:messageId)")
    fun setDeleted(messageId: String)

    @Query("UPDATE LoggedMessage SET deleted = true WHERE id IN :messageId")
    fun setDeleted(messageId: List<String>)

    interface AttachmentRepository : JpaRepository<LoggedMessage.MessageAttachments, String> {

    }
}

