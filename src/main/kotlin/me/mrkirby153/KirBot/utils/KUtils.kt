package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.Shard
import me.mrkirby153.KirBot.database.api.GuildMember
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
import java.io.InputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

fun File.child(path: String) = File(this, path)

fun File.readProperties(): Properties {
    return Properties().apply { load(this@readProperties.inputStream()) }
}

fun InputStream.readProperties(): Properties {
    return Properties().apply { load(this@readProperties) }
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
fun Member.getClearance() = this.user.getClearance(this.guild)

fun User.getMember(server: Guild) = server.getMember(this)

fun Guild.shard(): Shard? {
    return Bot.getShardForGuild(this.id)
}


@JvmOverloads
fun makeEmbed(title: String?, msg: String?, color: Color? = Color.WHITE, img: String? = null,
              thumb: String? = null, author: User? = null): MessageEmbed {
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

infix fun Any.botUrl(url: String): String {
    return Bot.constants.getProperty("bot-base-url") + "/" + url
}

fun TextChannel.hide() {
    this.permissionOverrides.filter { it.allowed.contains(Permission.MESSAGE_READ) }.forEach {
        val override = it
        it.manager.clear(Permission.MESSAGE_READ).queue {
            if (override.denied.isEmpty() && override.allowed.isEmpty())
                override.delete().queue()
        }
    }
    val public = this.getPermissionOverride(guild.publicRole) ?: this.createPermissionOverride(
            guild.publicRole).complete()
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
                                Bot.LOG.debug(
                                        "Permissions for roleId ${it.role.name} have changed. Updating")
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
                    g.members.map {
                        this.getMemberById(it)
                    }.filter { g.role !in it.roles }.forEach { u ->
                        this.controller.addRolesToMember(u, g.role).queue()
                    }
                }
                this.members.filter { g.role in it.roles }.filter { it.user.id !in g.members }.forEach {
                    this.controller.removeRolesFromMember(it, g.role).queue()
                }
            }
        }

        // Sync Members
        PanelAPI.getMembers(this).queue { members ->
            val toDelete = mutableListOf<GuildMember>()
            val currentMembers = this.members.map { it.user.id }
            members.forEach { m ->
                if (m.userId !in currentMembers) {
                    toDelete.add(m)
                }
            }
            Bot.LOG.debug("Deleting " + toDelete.map { it.userId })
            toDelete.forEach { m ->
                m.delete().queue()
            }

            // To register
            val newMembers = this.members.filter { it.user.id !in members.map { it.userId } }

            Bot.LOG.debug("Adding " + newMembers.map { it.user.id })

            newMembers.forEach {
                GuildMember.create(it).queue()
            }

            members.filter { it.needsUpdate() }.forEach {
                it.update().queue()
            }
        }
    }
}

fun Double.round(places: Int): Double {
    var format = "#.#"
    for (i in 0 until Math.min(places, 1) - 1) {
        format += "#"
    }
    return DecimalFormat(format, DecimalFormatSymbols(Locale.US)).format(this).toDouble()
}

fun String.mdEscape(): String {
    val pattern = Pattern.compile("\\*|\\[|]|_|~|\\(|\\)")

    val matcher = pattern.matcher(this)

    return buildString {
        var start = 0
        while (matcher.find()) {
            append(this@mdEscape.substring(start until matcher.start()))
            append("\\${this@mdEscape.substring(matcher.start() until matcher.end())}")
            start = matcher.end()
        }
        append(this@mdEscape.substring(start until this@mdEscape.length))
    }
}

fun Message.deleteAfter(time: Long, unit: TimeUnit) {
    if (this.channel.checkPermissions(Permission.MESSAGE_MANAGE))
        this.delete().queueAfter(time, unit)
}

fun <T : Channel> T.checkPermissions(vararg permission: Permission) = this.guild.selfMember.hasPermission(this, *permission)

fun MessageChannel.checkPermissions(vararg permissions: Permission) = (this as? TextChannel)?.checkPermissions<TextChannel>(*permissions) != false