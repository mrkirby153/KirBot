package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.MessageConcurrencyManager
import me.mrkirby153.KirBot.event.EventPriority
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.logger.LogPump
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.utils.Debouncer
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.KirBot.utils.sanitize
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.events.ShutdownEvent
import net.dv8tion.jda.api.events.channel.text.TextChannelCreateEvent
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateNameEvent
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelCreateEvent
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.voice.update.VoiceChannelUpdateNameEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.api.events.role.RoleCreateEvent
import net.dv8tion.jda.api.events.role.RoleDeleteEvent
import net.dv8tion.jda.api.events.role.update.RoleUpdateColorEvent
import net.dv8tion.jda.api.events.role.update.RoleUpdateHoistedEvent
import net.dv8tion.jda.api.events.role.update.RoleUpdateMentionableEvent
import net.dv8tion.jda.api.events.role.update.RoleUpdateNameEvent
import net.dv8tion.jda.api.events.role.update.RoleUpdatePermissionsEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent
import org.json.JSONObject
import javax.inject.Inject

class Logger @Inject constructor(private val redis: Redis): Module("logging") {

    private val logDelay = 100L

    val debouncer = Debouncer()

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

    @Periodic(120)
    fun clearDebounces() {
        debouncer.removeExpired()
    }

    @Subscribe
    fun onShutdown(event: ShutdownEvent?) {
        logPump.shutdown()
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
        event.guild.kirbotGuild.logManager.logMessageDelete(event.messageId)
        MessageConcurrencyManager.delete(event.messageId)
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
        event.guild.kirbotGuild.logManager.logBulkDelete(event.channel, event.messageIds)
        MessageConcurrencyManager.delete(*event.messageIds.toTypedArray())
    }

    @Subscribe
    fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        val roles = event.roles.filter {
            !debouncer.find(GuildMemberRoleAddEvent::class.java, Pair("user", event.user.id),
                    Pair("role", it.id))
        }
        if (roles.isNotEmpty())
            event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_ADD, ":key:",
                    "Assigned **${roles.joinToString(
                            ", ") { it.name }}** to ${event.user.logName}")
    }

    @Subscribe
    fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        val roles = event.roles.filter {
            !debouncer.find(GuildMemberRoleRemoveEvent::class.java, Pair("user", event.user.id),
                    Pair("role", it.id))
        }
        if (roles.isNotEmpty())
            event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_REMOVE, ":key:",
                    "Removed **${roles.joinToString(
                            ", ") { it.name }}** from ${event.user.logName}")
    }

    @Subscribe
    fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_JOIN, ":inbox_tray:",
                "${event.user.logName} joined (Created ${Time.formatLong(
                        System.currentTimeMillis() - (event.user.timeCreated.toEpochSecond() * 1000))} ago)")
    }

    @Subscribe
    fun onGuildMemberLeave(event: GuildMemberRemoveEvent) {
        if (debouncer.find(GuildMemberRemoveEvent::class.java, Pair("user", event.user.id),
                        Pair("guild", event.guild.id)))
            return
        val member = event.member
        val leaveString = buildString {
            append("${event.user.logName} left")
            if (member != null) {
                append(" (Joined ")
                val t = System.currentTimeMillis() - (member.timeJoined.toEpochSecond() * 1000)
                if (t < 1000) {
                    append("a moment ago")
                } else {
                    append("${Time.formatLong(t)} ago")
                }
                append(")")
            }
        }
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_LEAVE, ":outbox_tray:",
                leaveString)
    }

    @Subscribe
    fun onRoleCreate(event: RoleCreateEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_CREATE, ":hammer_pick:",
                "Role **${event.role.name}** (`${event.role.id}`) created")
    }

    @Subscribe
    fun onRoleDelete(event: RoleDeleteEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_DELETE, ":bomb:",
                "Role **${event.role.name}** (`${event.role.id}`) deleted")
    }

    @Subscribe
    fun onRoleUpdateName(event: RoleUpdateNameEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_UPDATE, ":wrench:",
                "Role **${event.oldName}** (`${event.role.id}`) renamed to **${event.role.name}**")
    }

    @Subscribe
    fun onRoleUpdateColor(event: RoleUpdateColorEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_UPDATE, ":wrench:",
                "Role **${event.role.name}** (`${event.role.id}`) changed color from #${event.oldColorRaw.toString(
                        16)} to #${event.newColorRaw.toString(16)}")
    }

    @Subscribe
    fun onRoleUpdateHoisted(event: RoleUpdateHoistedEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_UPDATE, ":wrench:",
                buildString {
                    append("Role **${event.role.name}** (`${event.role.id}`) ")
                    if (event.newValue) {
                        append(" was hoisted")
                    } else {
                        append(" was dehoisted")
                    }
                })
    }

    @Subscribe
    fun onRoleUpdateMentionable(event: RoleUpdateMentionableEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_UPDATE, ":wrench:",
                buildString {
                    append("Role **${event.role.name}** (`${event.role.id}`) ")
                    if (event.newValue) {
                        append(" was made mentionable")
                    } else {
                        append(" was made unmentionable")
                    }
                })
    }

    @Subscribe
    fun onRoleUpdatePermissions(event: RoleUpdatePermissionsEvent) {
        val granted = event.newPermissions.filter { it !in event.oldPermissions }.map { it.getName() }
        val revoked = event.oldPermissions.filter { it !in event.newPermissions }.map { it.getName() }
        if (granted.isNotEmpty() || revoked.isNotEmpty()) {
            event.guild.kirbotGuild.logManager.genericLog(LogEvent.ROLE_UPDATE, ":wrench:",
                    buildString {
                        append("Role **${event.role.name.sanitize()}** (`${event.role.id}`)")
                        if (granted.isNotEmpty()) {
                            append(" was granted `${granted.joinToString(", ")}`")
                        }
                        if (granted.isNotEmpty() && revoked.isNotEmpty())
                            append(" and")
                        if (revoked.isNotEmpty()) {
                            append(" had `${revoked.joinToString(", ")}` revoked")
                        }
                    })
        }
    }

    @Subscribe
    fun onTextChannelCreate(event: TextChannelCreateEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.CHANNEL_CREATE, ":pencil:",
                "Text channel #${event.channel.name.sanitize()} (`${event.channel.id}`) was created")
    }

    @Subscribe
    fun onVoiceChannelCreate(event: VoiceChannelCreateEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.CHANNEL_CREATE, ":pencil:",
                "Voice channel ${event.channel.name.sanitize()} (`${event.channel.id}`) was created")
    }

    @Subscribe
    fun onTextChannelDelete(event: TextChannelDeleteEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.CHANNEL_DELETE, ":wastebasket:",
                "Text channel #${event.channel.name.sanitize()} (`${event.channel.id}`) was deleted")
    }

    @Subscribe
    fun onVoiceChannelDelete(event: VoiceChannelDeleteEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.CHANNEL_DELETE, ":wastebasket:",
                "Voice channel ${event.channel.name.sanitize()} (`${event.channel.id}`) was deleted")
    }

    @Subscribe
    fun onTextRename(event: TextChannelUpdateNameEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.CHANNEL_MODIFY, ":pencil:",
                "#${event.oldName.sanitize()} (`${event.channel.id}`) was renamed to #${event.newName.sanitize()}")
    }

    @Subscribe
    fun onVoiceRename(event: VoiceChannelUpdateNameEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.CHANNEL_MODIFY, ":pencil:",
                "${event.oldName.sanitize()} (`${event.channel.id}`) was renamed to ${event.newName.sanitize()}")
    }

    @Subscribe
    fun onGuildMemberNickChange(event: GuildMemberUpdateNicknameEvent) {
        if (debouncer.find(GuildMemberUpdateNicknameEvent::class.java, Pair("id", event.user.id)))
            return
        when {
            event.oldNickname == null -> {
                event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_NICKNAME_CHANGE,
                        ":name_badge:",
                        "${event.user.logName} set nickname `${event.newNickname}`")
                return
            }
            event.oldNickname == null -> event.guild.kirbotGuild.logManager.genericLog(
                    LogEvent.USER_NICKNAME_CHANGE, ":name_badge:",
                    "${event.user.logName} removed nickname `${event.newNickname}`")
            else -> event.guild.kirbotGuild.logManager.genericLog(LogEvent.USER_NICKNAME_CHANGE,
                    ":name_badge:",
                    "${event.user.logName} changed nick from `${event.oldNickname}` to `${event.newNickname}`")
        }
    }

    @Subscribe
    fun onUserUpdateName(event: UserUpdateNameEvent) {
        Bot.shardManager.shards.forEach { shard ->
            shard.guilds.forEach { guild ->
                if (event.user.id in guild.members.map { it.user.id } && guild.kirbotGuild.ready)
                    guild.kirbotGuild.logManager.genericLog(LogEvent.USER_NAME_CHANGE,
                            ":name_badge:",
                            "${event.oldName}#${event.user.discriminator} (`${event.user.id}`) changed username to **${event.user.nameAndDiscrim}**")
            }
        }
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        if (event.message.contentDisplay.isEmpty())
            return
        event.guild.kirbotGuild.logManager.logEdit(event.message)
        MessageConcurrencyManager.update(event.message)
    }

    @Subscribe(priority = EventPriority.HIGHEST)
    fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (!event.guild.kirbotGuild.ready)
            return
        MessageConcurrencyManager.insert(event.message)
    }

    @Subscribe
    fun onGuildVoiceJoin(event: GuildVoiceJoinEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.VOICE_ACTION, ":telephone:",
                "${event.member.user.logName} joined **${event.channelJoined.name}**")
    }

    @Subscribe
    fun onGuildVoiceLeave(event: GuildVoiceLeaveEvent) {
        event.guild.kirbotGuild.logManager.genericLog(LogEvent.VOICE_ACTION, ":telephone:",
                "${event.member.user.logName} left **${event.channelLeft.name}**")
    }

    @Subscribe
    fun onGuildVoiceMove(event: GuildVoiceMoveEvent) {
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
       redis.getConnection().use {
            it.set("log_events", obj.toString())
        }
    }
}