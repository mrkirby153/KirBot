package me.mrkirby153.KirBot.server

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.elements.Pair
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import org.slf4j.event.Level
import java.util.LinkedList

class BackfillTask(val guild: Guild, val id: String, val type: BackfillType) : Runnable {
    var scanned = 0
    var updated = 0
    var created = 0
    var deleted = 0
    var duration = 0L
    var startTime = 0L

    var callback: ((BackfillTask) -> Unit)? = null

    override fun run() {
        log("Starting backfill")
        startTime = System.currentTimeMillis()
        when (type) {
            BackfillType.CHANNEL -> {
                val c = Bot.shardManager.getTextChannel(id)
                if (c == null) {
                    log("Attempting to backfill a channel $id that wasn't found", Level.WARN)
                    finish()
                    return
                } else {
                    backfillChannel(c)
                }
            }
            BackfillTask.BackfillType.MESSAGE -> {
                val mid = this.id
                var msg: Message? = null
                guild.textChannels.forEach { chan ->
                    if (chan.checkPermissions(Permission.MESSAGE_HISTORY,
                                    Permission.MESSAGE_READ)) {
                        try {
                            val m = chan.getMessageById(mid).complete()
                            if (m != null)
                                msg = m
                        } catch (e: Exception) {
                            //Ignore
                        }
                    }
                }
                val existing = Model.where(GuildMessage::class.java, "id", mid).first()
                scanned++
                if (existing == null) {
                    if (msg != null) {
                        created++
                        GuildMessage(msg).save()
                    }
                } else {
                    if (msg == null) {
                        existing.deleted = true
                        existing.save()
                        deleted++
                    } else if (existing.message != msg!!.contentRaw) {
                        updated++
                        existing.message = msg!!.contentRaw
                        existing.editCount++
                        existing.save()
                    }
                }
            }
            BackfillTask.BackfillType.GUILD -> {
                backfillGuild()
            }
        }
        finish()
    }

    fun finish() {
        val end = System.currentTimeMillis()
        this.duration = end - startTime
        log("Finished in ${Time.format(0, duration)}")
        callback?.invoke(this)
    }

    private fun backfillGuild() {
        val guild = Bot.shardManager.getGuild(this.id)
        if (guild == null) {
            log("Attempting to backfill guild ${this.id} but it was not found. Aborting",
                    Level.WARN)
            return
        }
        if (guild.textChannels.isEmpty()) {
            log("Attempting to backfill guild ${this.id} but it has no text channels. Aborting")
            return
        }
        val channels = LinkedList<String>(guild.textChannels.map { it.id })
        var currentChannel: String = channels.pop()
        fun getNextChannel() {
            currentChannel = if (channels.peek() != null) {
                channels.pop()
            } else {
                ""
            }
        }
        while (currentChannel != "") {
            val chan = guild.getTextChannelById(currentChannel)
            backfillChannel(chan)
            getNextChannel()
        }
    }

    private fun backfillChannel(chan: TextChannel): Boolean {
        if (!chan.checkPermissions(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY)) {
            log("Skipping #${chan.name} -- No permission to read history", Level.DEBUG)
            return false
        } else {
            log("Backfilling ${chan.name}")
        }

        val loggedMessages = Model.where(GuildMessage::class.java, "channel", chan.id).get()

        log("${loggedMessages.size} messages stored", Level.DEBUG)
        val loggedMsgMap = mutableMapOf<String, GuildMessage>()
        log("Transforming message map", Level.DEBUG)
        loggedMessages.forEach { e ->
            loggedMsgMap[e.id] = e
        }
        val modified = mutableListOf<Message>()
        val created = mutableListOf<Message>()
        val chanMsgs = mutableListOf<String>()

        log("Iterating channel history", Level.DEBUG)
        chan.iterableHistory.forEach { msg ->
            scanned++
            chanMsgs.add(msg.id)
            val stored = loggedMsgMap[msg.id]
            if (stored != null) {
                if (stored.message != msg.contentRaw) {
                    modified.add(msg)
                    this.updated++
                }
            } else {
                created.add(msg)
                this.created++
            }
        }
        log("Determining deleted messages", Level.DEBUG)
        val deleted = loggedMsgMap.keys.filter { it !in chanMsgs }
        log("Inserting ${created.size} messages", Level.DEBUG)
        created.forEach {
            // TODO 10/16/18 Use bulk insertion so it'll be faster
            GuildMessage(it).save()
        }
        log("Updating ${modified.size} messages", Level.DEBUG)
        modified.forEach {
            val msg = loggedMsgMap[it.id] ?: return@forEach
            msg.editCount++
            msg.message = it.contentRaw
            msg.save()
        }
        val toDelete = deleted.filter { !(loggedMsgMap[it]?.deleted ?: true) }
        log("Deleting ${toDelete.size} messages", Level.DEBUG)
        if (toDelete.isNotEmpty()) {
            Model.query(GuildMessage::class.java).whereIn("id", toDelete.toTypedArray()).update(
                    Pair("deleted", true))
        }
        return true
    }

    private class BackfilledMessage(msg: Message, val type: Type) : Message by msg {
        enum class Type {
            CREATED,
            UPDATED
        }
    }

    private fun log(msg: String, level: Level = Level.INFO) {
        val m = "[BACKFILL_$type/$id] $msg"
        when (level) {
            Level.DEBUG -> Bot.LOG.debug(m)
            Level.INFO -> Bot.LOG.info(m)
            Level.ERROR -> Bot.LOG.error(m)
            Level.WARN -> Bot.LOG.warn(m)
            Level.TRACE -> Bot.LOG.trace(m)
        }
    }

    enum class BackfillType {
        CHANNEL,
        MESSAGE,
        GUILD
    }
}