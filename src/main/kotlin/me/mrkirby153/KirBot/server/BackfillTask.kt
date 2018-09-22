package me.mrkirby153.KirBot.server

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.DB
import com.mrkirby153.bfs.sql.elements.Pair
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import java.util.LinkedList

class BackfillTask(private val guild: KirBotGuild) : Runnable {
    private val channelsLeft = LinkedList<String>()

    private var currentChannel: String = ""

    var updated = 0
    var created = 0
    var deleted = 0

    var callback: ((BackfillTask) -> Unit)? = null

    init {
        channelsLeft.addAll(guild.textChannels.map { it.id })
    }

    override fun run() {
        Bot.LOG.info("Starting complete backfill on ${this.guild}")
        getNextChannel()
        val start = System.currentTimeMillis()
        while (this.currentChannel != "") {
            val channel = guild.getTextChannelById(this.currentChannel)
            if (channel == null) {
                getNextChannel()
                continue
            }
            if (!channel.checkPermissions(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY)) {
                Bot.LOG.debug(
                        "[BACKFILL/${guild.id}] Skipping #${channel.name} -- No permission to read messages")
                getNextChannel()
                continue
            } else {
                Bot.LOG.debug("[BACKFILL/${guild.id}] Backfilling #${channel.name}")
            }

            val loggedMessages = Model.where(GuildMessage::class.java, "channel", channel.id).get()
            val loggedMids = DB.getFirstColumnValues<String>("SELECT `id` FROM server_messages WHERE channel = ?", channel.id)

            val loggedMessageMap = mutableMapOf<String, GuildMessage>()
            loggedMessages.forEach { loggedMessageMap[it.id] = it }
            val chanMessages = mutableListOf<String>()
            val modified = mutableListOf<BackfilledMessage>()
            channel.iterableHistory.forEach { msg ->
                chanMessages.add(msg.id)
                if (loggedMessageMap.containsKey(msg.id)) {
                    val existingMsg = loggedMessageMap[msg.id] ?: return@forEach
                    if (existingMsg.message != msg.contentRaw) {
                        modified.add(BackfilledMessage(msg, BackfilledMessage.Type.UPDATED))
                    }
                } else {
                    modified.add(BackfilledMessage(msg, BackfilledMessage.Type.CREATED))
                }
            }
            val deleted = loggedMids.filter { it !in chanMessages }

            Bot.LOG.debug(
                    "[BACKFILL/${guild.id}] Found ${modified.size} new or modified messages and ${deleted.count()} deleted messages")
            modified.filter { it.type == BackfilledMessage.Type.UPDATED }.forEach { modifiedMsg ->
                val existing = loggedMessages.first { it.id == modifiedMsg.id }
                existing.editCount++
                existing.message = modifiedMsg.contentRaw
                existing.save()
            }
            modified.filter { it.type == BackfilledMessage.Type.CREATED }.forEach { createdMsg ->
                val msg = GuildMessage(createdMsg)
                msg.save()
            }
            val toDelete = mutableListOf<String>()
            if (deleted.isNotEmpty()) {
                deleted.forEach { id ->
                    val logged = loggedMessageMap[id] ?: return@forEach
                    if (!logged.deleted) {
                        toDelete.add(id)
                    }
                }
                if (toDelete.isNotEmpty())
                    Model.query(GuildMessage::class.java).whereIn("id",
                            toDelete.toTypedArray()).update(
                            Pair("deleted", true))
            }
            this.updated += modified.asSequence().filter { it.type == BackfilledMessage.Type.UPDATED }.count()
            this.deleted += toDelete.count()
            this.created += modified.asSequence().filter { it.type == BackfilledMessage.Type.CREATED }.count()
            getNextChannel()
        }
        val end = System.currentTimeMillis()
        Bot.LOG.info(
                "[BACKFILL/${guild.id}] Backfill finished in ${Time.format(1,
                        end - start)}. $created created, $updated updated, $deleted deleted")
        callback?.invoke(this)
    }

    private fun getNextChannel() {
        Bot.LOG.debug("[BACKFIll/${guild.id}] Backfilling next channel...")
        this.currentChannel = if (channelsLeft.peek() != null) channelsLeft.pop() else ""
    }

    private class BackfilledMessage(msg: Message, val type: Type) : Message by msg {
        enum class Type {
            CREATED,
            UPDATED
        }
    }
}