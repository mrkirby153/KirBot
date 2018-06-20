package me.mrkirby153.KirBot.modules

import com.mrkirby153.bfs.Tuple
import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.DB
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.logger.LogPump
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.core.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.core.events.role.RoleCreateEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.events.role.update.RoleUpdateNameEvent
import net.dv8tion.jda.core.events.user.UserNameUpdateEvent
import org.json.JSONObject

class Logger : Module("logging") {

    private val logDelay = 1000L

    init {
        dependencies.add(Database::class.java)
        dependencies.add(Redis::class.java)
    }

    private lateinit var logPump: LogPump

    override fun onLoad() {
        log("Starting logger....")
        logPump = LogPump(logDelay)
        logPump.start()
        registerLogEvents()
    }

    override fun onShutdown(event: ShutdownEvent?) {
        logPump.shutdown()
    }

    override fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
        event.guild.kirbotGuild.logManager.logMessageDelete(event.messageId)
        val msg = Model.first(GuildMessage::class.java, Tuple("id", event.messageId)) ?: return
        msg.deleted = true
        msg.save()
    }

    override fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        event.guild.kirbotGuild.logManager.logBulkDelete(event.channel, event.messageIds)
        val selector = "?, ".repeat(event.messageIds.size)
        DB.executeUpdate(
                "UPDATE `server_messages` SET `deleted` = TRUE WHERE `id` IN (${selector.substring(
                        0, selector.lastIndexOf(","))})", *(event.messageIds.toTypedArray()))
    }

    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_ADD, ":key:",
                "Assigned **${event.roles.joinToString(
                        ", ") { it.name }}** to ${event.user.logName}")
    }

    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_REMOVE, ":key:",
                "Removed **${event.roles.joinToString(
                        ", ") { it.name }}** from ${event.user.logName}")
    }

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_JOIN, ":inbox_tray:",
                "${event.user.logName} Joined (Created ${Time.formatLong(
                        System.currentTimeMillis() - (event.user.creationTime.toEpochSecond() * 1000))} ago)")
    }

    override fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_LEAVE, ":outbox_tray:",
                "${event.user.logName} left")
    }

    override fun onRoleCreate(event: RoleCreateEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_CREATE, ":hammer_pick:",
                "Role **${event.role.name}** (`${event.role.id}`) created")
    }

    override fun onRoleDelete(event: RoleDeleteEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_DELETE, ":bomb:",
                "Role **${event.role.name}** (`${event.role.id}`) deleted")
    }

    override fun onRoleUpdateName(event: RoleUpdateNameEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_UPDATE, ":wrench:",
                "Role **${event.oldName}** (`${event.role.id}`) renamed to **${event.role.name}**")
    }

    override fun onGuildMemberNickChange(event: GuildMemberNickChangeEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        when {
            event.prevNick == null -> {
                event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_NICKNAME_CHANGE,
                        ":name_badge:",
                        "${event.user.logName} Set nickname `${event.newNick}`")
                return
            }
            event.newNick == null -> event.guild.kirbotGuild.logManager.genericLog(
                    LogEvent.USER_NICKNAME_CHANGE, ":name_badge:",
                    "${event.user.logName} Removed nickname `${event.prevNick}`")
            else -> event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_NICKNAME_CHANGE,
                    ":name_badge:",
                    "${event.user.logName} changed nick from `${event.prevNick}` to `${event.newNick}`")
        }
    }

    override fun onUserNameUpdate(event: UserNameUpdateEvent) {
        Bot.shardManager.shards.forEach { shard ->
            shard.guilds.forEach { guild ->
                if (event.user.id in guild.members.map { it.user.id } && guild.kirbotGuild.ready)
                    guild.kirbotGuild.logManager.genericLog(LogEvent.USER_NAME_CHANGE,
                            ":name_badge:",
                            "${event.oldName}#${event.oldDiscriminator} (`${event.user.id}`) changed username to **${event.user.nameAndDiscrim}**")
            }
        }
    }

    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        if (event.message.contentDisplay.isEmpty())
            return
        event.guild.kirbotGuild.logManager.logEdit(event.message)
        val msg = Model.first(GuildMessage::class.java, Tuple("id", event.messageId)) ?: return
        msg.message = LogManager.encrypt(event.message.contentRaw)
        msg.editCount++
        msg.save()
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        if (event.message.contentRaw.isNotEmpty() || event.message.attachments.size > 0) {
            val msg = GuildMessage()
            msg.id = event.message.id
            msg.serverId = event.guild.id
            msg.author = event.author.id
            msg.channel = event.channel.id
            msg.message = LogManager.encrypt(event.message.contentRaw)
            if (event.message.attachments.size > 0)
                msg.attachments = event.message.attachments.joinToString(", ") { it.url }
            msg.save()
        }
    }

    override fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.VOICE_ACTION, ":telephone:",
                "${event.member.user.logName} joined **${event.channelJoined.name}**")
    }

    override fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.VOICE_ACTION, ":telephone:",
                "${event.member.user.logName} left **${event.channelLeft.name}**")
    }

    override fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.VOICE_ACTION, ":telephone:",
                "${event.member.user.logName} moved from **${event.channelLeft.name}** to **${event.channelJoined.name}**")
    }

    /**
     * Registers the available [LogEvent]s in redis for the panel
     */
    private fun registerLogEvents() {
        val obj = JSONObject()
        LogEvent.values().sorted().forEach {
            obj.put(it.toString(), it.permission)
        }
        Bot.LOG.debug("Registering log events in redis: $obj")
        ModuleManager[Redis::class.java].getConnection().use {
            it.set("log_events", obj.toString())
        }
    }
}