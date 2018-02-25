package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.core.events.message.MessageBulkDeleteEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.core.events.role.RoleCreateEvent
import net.dv8tion.jda.core.events.role.RoleDeleteEvent
import net.dv8tion.jda.core.events.user.UserNameUpdateEvent

class Logger : Module("logging") {

    init {
        dependencies.add(Database::class.java)
    }

    override fun onLoad() {
        log("Starting logger....")
    }

    override fun onGuildMessageDelete(event: GuildMessageDeleteEvent) {
        event.guild.kirbotGuild.logManager.logMessageDelete(event.messageId)
        Model.first(GuildMessage::class.java, Pair("id", event.messageId))?.delete()
    }

    override fun onMessageBulkDelete(event: MessageBulkDeleteEvent) {
        event.guild.kirbotGuild.logManager.logBulkDelete(event.channel, event.messageIds)
        val query = "DELETE FROM `server_messages` WHERE `id` IN (${event.messageIds.joinToString(
                ",") { "'$it'" }})"
        ModuleManager[Database::class.java].database.getConnection().use { conn ->
            conn.prepareStatement(query).use { ps ->
                ps.execute()
            }
        }
    }

    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        event.guild.kirbotGuild.logManager.genericLog(":key:",
                "Assigned **${event.roles.joinToString(
                        ", ") { it.name }}** to ${event.user.nameAndDiscrim}")
    }

    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        event.guild.kirbotGuild.logManager.genericLog(":key:",
                "Removed **${event.roles.joinToString(
                        ", ") { it.name }}** from ${event.user.nameAndDiscrim}")
    }

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        event.guild.kirbotGuild.logManager.genericLog(":inbox_tray:",
                "${event.user.nameAndDiscrim} (`${event.user.id}`) Joined (Created ${Time.format(1,
                        System.currentTimeMillis() - (event.user.creationTime.toEpochSecond() * 1000))} ago)")
    }

    override fun onGuildMemberLeave(event: GuildMemberLeaveEvent) {
        event.guild.kirbotGuild.logManager.genericLog(":outbox_tray:",
                "${event.user.nameAndDiscrim} (`${event.user.id}`) left")
    }

    override fun onRoleCreate(event: RoleCreateEvent) {
        event.guild.kirbotGuild.logManager.genericLog(":tools:",
                "Role **${event.role.name}** created")
    }

    override fun onRoleDelete(event: RoleDeleteEvent) {
        event.guild.kirbotGuild.logManager.genericLog(":bomb:",
                "Role **${event.role.name}** deleted")
    }

    override fun onGuildMemberNickChange(event: GuildMemberNickChangeEvent) {
        val prev = if (event.prevNick.isNullOrBlank()) "None" else event.prevNick
        val new = if (event.newNick.isNullOrBlank()) "None" else event.newNick
        event.guild.kirbotGuild.logManager.genericLog(":floppy_disk:",
                "${event.user.nameAndDiscrim} (`${event.user.id}`) changed nick from `$prev` to `$new`")
    }

    override fun onUserNameUpdate(event: UserNameUpdateEvent) {
        Bot.shardManager.shards.forEach { shard ->
            shard.guilds.forEach { guild ->
                guild.kirbotGuild.logManager.genericLog(":briefcase:",
                        "${event.oldName}#${event.oldDiscriminator} (`${event.user.id}`) changed username to **${event.user.nameAndDiscrim}**")
            }
        }
    }

    override fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        if (event.message.contentDisplay.isEmpty())
            return
        event.guild.kirbotGuild.logManager.logEdit(event.message)
        val msg = Model.first(GuildMessage::class.java, Pair("id", event.messageId)) ?: return
        msg.message = event.message.contentDisplay
        msg.save()
    }

    override fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.channel.id == event.guild.kirbotGuild.logManager.logChannel?.id) {
            return
        }
        if (event.message.contentDisplay.isNotEmpty()) {
            val msg = GuildMessage()
            msg.id = event.message.id
            msg.serverId = event.guild.id
            msg.author = event.author.id
            msg.channel = event.channel.id
            msg.message = event.message.contentDisplay
            msg.save()
        }
    }
}