package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.database.api.GuildRole
import me.mrkirby153.KirBot.database.api.GuildSettings
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.realname.RealnameHandler
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import java.awt.Color
import java.io.File
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

fun File.child(path: String) = File(this, path)

fun File.readProperties(): Properties {
    return Properties().apply { load(this@readProperties.inputStream()) }
}

fun File.createFileIfNotExist(): File {
    if (!this.exists())
        this.createNewFile()
    return this
}

fun File.mkdirIfNotExist(): File {
    if (!this.exists())
        this.mkdir()
    return this
}

fun User.getClearance(server: Guild): Clearance {
    if (Bot.admins.contains(this.id))
        return Clearance.BOT_OWNER
    if (server.getMember(this).isOwner)
        return Clearance.SERVER_OWNER
    if (server.getMember(this).permissions.contains(Permission.ADMINISTRATOR))
        return Clearance.SERVER_ADMINISTRATOR
    val shard = Bot.getShardForGuild(server.id)
    if (shard != null) {
        val managerRoles = shard.serverSettings[server.id]?.managerRoles
        if (managerRoles != null)
            server.getMember(this).roles.map { it.id }.forEach { role ->
                if (role in managerRoles) {
                    return Clearance.BOT_MANAGER
                }
            }
    }
    return Clearance.USER
}

fun Member.getClearance(server: Guild): Clearance = this.user.getClearance(server)

fun User.getMember(server: Guild) = server.getMember(this)

fun Guild.shard(): Shard? {
    return Bot.getShardForGuild(this.id)
}


@JvmOverloads
fun makeEmbed(title: String?, msg: String?, color: Color? = Color.WHITE, img: String? = null, thumb: String? = null, author: User? = null): MessageEmbed {
    return EmbedBuilder().run {
        setDescription(msg)
        setTitle(title, null)
        setColor(color)

        if (author != null) {
            setAuthor(author.name, null, author.avatarUrl)
        }
        setThumbnail(thumb)
        setImage(img)
        build()
    }
}

fun localizeTime(time: Int): String {
    if (time < 60) {
        return "$time seconds"
    } else if (time < 3600) {
        return "${roundTime(2, time.toDouble() / 60)} minutes"
    } else if (time < 86400) {
        return "${(roundTime(2, time.toDouble() / 3600))} hours"
    } else if (time < 604800) {
        return "${roundTime(2, time.toDouble() / 86400)} days"
    } else {
        return "${roundTime(2, time.toDouble() / 604800)} weeks"
    }
}

fun roundTime(degree: Int, number: Double): Double {
    if (degree == 0)
        return Math.round(number).toDouble()
    var format = "#.#"
    for (i in (1..degree - 1))
        format += "#"

    val sym = DecimalFormatSymbols(Locale.US)
    val twoDform = DecimalFormat(format, sym)
    return twoDform.format(number).toDouble()
}

inline fun <T : AutoCloseable, R> T.use(block: (T) -> R): R {
    var closed = false
    try {
        return block(this)
    } catch (e: Exception) {
        closed = true
        try {
            this.close()
        } catch (closeException: Exception) {
        }
        throw e
    } finally {
        if (!closed) {
            this.close()
        }
    }
}

fun TextChannel.hide() {
    this.permissionOverrides.filter { it.allowed.contains(Permission.MESSAGE_READ) }.forEach {
        val override = it
        it.manager.clear(Permission.MESSAGE_READ).queue {
            if (override.denied.isEmpty() && override.allowed.isEmpty())
                override.delete().queue()
        }
    }
    val public = this.getPermissionOverride(guild.publicRole) ?: this.createPermissionOverride(guild.publicRole).complete()
    public.manager.deny(Permission.MESSAGE_READ).queue()
    PanelAPI.getChannels(this.guild).queue {
        it.text.filter { it.id == this.id }.forEach { it.update().queue() }
    }
}

fun TextChannel.unhide() {
    val public = this.getPermissionOverride(guild.publicRole) ?: return
    public.manager.clear(Permission.MESSAGE_READ).queue()
    PanelAPI.getChannels(this.guild).queue {
        it.text.filter { it.id == this.id }.forEach { it.update().queue() }
    }
}

fun Guild.sync() {
    Bot.LOG.debug("Syncing guild ${this.id}")
    PanelAPI.serverExists(this).queue { exists ->
        if (!exists)
            PanelAPI.registerServer(this).queue {
                this.sync()
            }
        else
            GuildSettings.get(this).queue { settings ->
                if (this.selfMember.nickname != settings.nick) {
                    Bot.LOG.debug("Updating nickname to \"${settings.nick}\"")
                    if (settings.nick?.isEmpty() == true)
                        this.controller.setNickname(this.selfMember, null).queue()
                    else
                        this.controller.setNickname(this.selfMember, settings.nick).queue()
                }
                if (settings.name != this.name) {
                    Bot.LOG.debug("Name has changed on ${this.name} syncing")
                    PanelAPI.setServerName(this).queue()
                }
                PanelAPI.updateChannels(this)

                PanelAPI.getRoles(this).queue { r ->

                    val storedRoleIds = r.map { it.id }

                    r.forEach {
                        if (it.role != null) {
                            val guildPermissions = it.role.permissionsRaw
                            val storedPermissions = it.permissions
                            if (guildPermissions != storedPermissions) {
                                Bot.LOG.debug("Permissions for roleId ${it.role.name} have changed. Updating")
                                GuildRole.get(it.role).queue {
                                    it.update().queue()
                                }
                            }
                        }
                    }

                    val toAdd = mutableListOf<String>()
                    val toRemove = mutableListOf<String>()

                    toRemove.addAll(storedRoleIds)

                    toRemove.removeAll(this.roles.map { it.id })
                    toAdd.addAll(this.roles.filter { it.id !in storedRoleIds }.map { it.id })

                    Bot.LOG.debug("Adding roles $toAdd")
                    Bot.LOG.debug("Removing roles $toRemove")

                    toAdd.map { this.getRoleById(it) }.filter { it != null }.forEach { role ->
                        GuildRole.create(role).queue()
                    }

                    toRemove.forEach { role ->
                        GuildRole.delete(role).queue()
                    }


                }
            }
        RealnameHandler(this, shard()!!.getServerData(this)).updateNames()

        // Sync groups
        PanelAPI.getGroups(this).queue { group ->
            group.forEach { g ->
                if (g.role != null) {
                    g.members.map { this.getMemberById(it) }.filter { g.role !in it.roles }.forEach { u ->
                        this.controller.addRolesToMember(u, g.role).queue()
                    }
                }
                this.members.filter { g.role in it.roles }.filter { it.user.id !in g.members }.forEach {
                    this.controller.removeRolesFromMember(it, g.role).queue()
                }
            }
        }
    }
}