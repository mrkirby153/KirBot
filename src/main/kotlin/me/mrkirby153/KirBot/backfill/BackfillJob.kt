package me.mrkirby153.KirBot.backfill

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
import java.util.LinkedList
import kotlin.system.measureTimeMillis

class BackfillJob(val jobId: String, val guild: Guild, val id: String, val jobType: JobType,
                  val maxMessages: Long = -1) : Runnable {

    private val logMessages = mutableListOf<String>()
    private val messageSubscribers = mutableListOf<(String) -> Unit>()
    var onComplete: (() -> Unit)? = null
    lateinit var thread: Thread

    override fun run() {
        try {
            backfill()
        } catch (e: InterruptedException) {
            // Eat
        }
    }

    fun backfill() {
        val time = measureTimeMillis {
            when (jobType) {
                JobType.GUILD -> {
                    log("Starting backfill of guild `$id`")
                    backfillGuild()
                }
                JobType.CHANNEL -> {
                    log("Starting backfill of channel `$id`")
                    val chan = guild.getTextChannelById(id)
                    if(chan == null) {
                        log(":warning: Channel `$id` does not exist")
                    } else {
                        backfillChannel(chan)
                    }
                }
                JobType.MESSAGE -> {
                    log("Starting backfill of message `$id`")
                    backfillMessage()
                }
            }
        }
        log("Backfill finished in ${Time.format(1, time)}")
        onComplete?.invoke()
    }


    fun backfillGuild() {
        val guild = Bot.shardManager.getGuild(this.id)
        if (guild == null) {
            log(":warning: Guild `${this.id}` was not found")
            return
        }
        val remainingChannels = LinkedList<String>(guild.textChannels.map { it.id })
        var currentChannel = ""
        fun getNextChannel() {
            currentChannel = if (remainingChannels.peek() != null) {
                remainingChannels.pop()
            } else {
                ""
            }
        }
        getNextChannel()
        while (currentChannel != "") {
            log("Backfilling <#$currentChannel> (`$currentChannel`) [${remainingChannels.size} left]")
            val c = guild.getTextChannelById(currentChannel) ?: continue
            backfillChannel(c)
            getNextChannel()
            if(Thread.interrupted()) {
                log("Interrupted!")
                return
            }
        }
    }

    fun backfillChannel(channel: TextChannel) {
        if (!channel.checkPermissions(Permission.MESSAGE_READ, Permission.MESSAGE_HISTORY)) {
            return // Can't read the channel
        }

        val existingLoggedMessages = Model.where(GuildMessage::class.java, "channel",
                channel.id).get()

        // Throw all the messages into a lookup table
        val loggedMsgMap = mutableMapOf<String, GuildMessage>()
        existingLoggedMessages.forEach { e ->
            loggedMsgMap[e.id] = e
        }

        val modified = mutableListOf<Message>()
        val created = mutableListOf<Message>()
        val chanMessages = mutableListOf<String>()
        var scanned = 0

        log("Scanning messages in <#${channel.id}> (`${channel.id}`)")
        val time = measureTimeMillis {
            channel.iterableHistory.forEach { message ->
                scanned++
                chanMessages.add(message.id)
                val stored = loggedMsgMap[message.id]
                if (stored != null) {
                    if (stored.message != message.contentRaw) {
                        modified.add(message)
                    }
                } else {
                    created.add(message)
                }
                if(this.maxMessages != -1L && scanned >= maxMessages) {
                    log("Reached message cap for <#${channel.id}> (`${channel.id}`)!")
                    return@forEach
                }
                if(Thread.interrupted()) {
                    log("Interrupted!")
                    return
                }
            }
        }
        log("Scanned $scanned messages from <#${channel.id}> (`${channel.id}`) in ${Time.format(1,
                time)}")
        val deleted = loggedMsgMap.keys.filter { it !in chanMessages }
        log("Inserting ${created.size}, Deleting ${deleted.size} and updating ${modified.size} messages in <#${channel.id}> (`${channel.id}`)")
        created.forEach {
            GuildMessage(it).save()
            if(Thread.interrupted()) {
                log("Interrupted!")
                return
            }
        }
        modified.forEach {
            val msg = loggedMsgMap[it.id] ?: return@forEach
            msg.editCount++
            msg.message = it.contentRaw
            msg.save()
            if(Thread.interrupted()) {
                log("Interrupted!")
                return
            }
        }
        val toDelete = deleted.filter {
            !(loggedMsgMap[it]?.deleted ?: true)
        } // Ignore already deleted messages
        if (toDelete.isNotEmpty()) {
            Model.query(GuildMessage::class.java).whereIn("id", toDelete.toTypedArray()).update(
                    Pair("deleted", true))
        }
        log("Backfill of <#${channel.id}> (`${channel.id}`) completed")
    }

    fun backfillMessage() {
        var msg: Message? = null
        guild.textChannels.forEach { chan ->
            if(chan.checkPermissions(Permission.MESSAGE_HISTORY, Permission.MESSAGE_READ)) {
                try{
                    val m = chan.getMessageById(this.id).complete()
                    if(m != null) {
                        msg = m
                        return@forEach
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
            if(Thread.interrupted()) {
                log("Interrupted!")
                return
            }
        }
        val existing = Model.where(GuildMessage::class.java, "id", this.id).first()
        if(existing == null) {
            if(msg != null) {
                // The message exists, but isn't in the DB
                GuildMessage(msg).save()
            }
        } else {
            // The message exists in the DB
            if(msg == null) {
                // The message was deleted, delete it in the DB
                existing.deleted = true
                existing.save()
            } else if(existing.message != msg!!.contentRaw) {
                // The message's content has changed
                existing.message = msg!!.contentRaw
                existing.editCount++
                existing.save()
            }
        }
    }


    fun log(message: String) {
        val msg = "[`${this.jobId}`] $message"
        this.logMessages.add(msg)
        this.messageSubscribers.forEach { it.invoke(msg) }
    }

    fun getLogMessages() = logMessages.toTypedArray()

    fun subscribe(subscriber: (String) -> Unit) {
        messageSubscribers.add(subscriber)
    }

    enum class JobType {
        GUILD,
        MESSAGE,
        CHANNEL
    }
}