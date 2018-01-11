package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.api.PanelAPI
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.sharding.Shard
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.awt.Color
import java.io.InputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

fun InputStream.readProperties(): Properties {
    return Properties().apply { load(this@readProperties) }
}

fun User.getClearance(server: Guild): Clearance {
    if (Bot.admins.contains(this.id))
        return Clearance.BOT_OWNER
    if (server.getMember(this).isOwner)
        return Clearance.SERVER_OWNER
    if (server.getMember(this).permissions.contains(Permission.ADMINISTRATOR))
        return Clearance.SERVER_ADMINISTRATOR
    val shard = Bot.shardManager.getShard(server)
    if (shard != null) {
        val managerRoles = server.kirbotGuild.settings.managerRoles
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
    return Bot.shardManager.getShard(this)
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

val Guild.kirbotGuild
    get() = KirBotGuild[this]

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

fun <T : Channel> T.checkPermissions(
        vararg permission: Permission) = this.guild.selfMember.hasPermission(this, *permission)

fun MessageChannel.checkPermissions(
        vararg permissions: Permission) = (this as? TextChannel)?.checkPermissions<TextChannel>(
        *permissions) != false

