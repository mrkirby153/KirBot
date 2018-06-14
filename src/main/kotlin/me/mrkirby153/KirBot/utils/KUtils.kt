package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.listener.WaitUtils
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.sharding.Shard
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import org.json.JSONArray
import java.awt.Color
import java.io.InputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

fun InputStream.readProperties(): Properties {
    return Properties().apply { load(this@readProperties) }
}

fun User.getClearance(server: Guild) = server.kirbotGuild.getClearance(this)

fun User.getMember(server: Guild) = server.getMember(this)

fun User.canInteractWith(guild: Guild, user: User): Boolean = this.getClearance(guild) > user.getClearance(guild)

fun Guild.shard(): Shard? {
    return Bot.shardManager.getShard(this)
}

fun uploadToArchive(text: String, ttl: Int = 604800): String {
    ModuleManager[Redis::class.java].getConnection().use {
        val key = UUID.randomUUID().toString()
        Bot.LOG.debug("Archive created $key (Expires in ${Time.formatLong(ttl * 1000L,
                Time.TimeUnit.SECONDS).toLowerCase()})")
        it.set("archive:$key", text)
        it.expire("archive:$key", ttl)
        return String.format(Bot.properties.getProperty("archive-base"), key)
    }
}

fun promptForConfirmation(context: Context, msg: String, onConfirm: (() -> Boolean)? = null,
                          onDeny: (() -> Boolean)? = null) {
    context.channel.sendMessage(msg).queue { m ->
        m.addReaction(GREEN_TICK.emote).queue()
        m.addReaction(RED_TICK.emote).queue()
        WaitUtils.waitFor(MessageReactionAddEvent::class.java, {
            if (it.user.id != context.author.id)
                return@waitFor
            if(it.messageId != m.id)
                return@waitFor
            if (it.reactionEmote.isEmote) {
                when (it.reactionEmote.id) {
                    GREEN_TICK.id -> {
                        if (onConfirm == null || onConfirm.invoke())
                            m.delete().queue()
                        cancel()
                    }
                    RED_TICK.id -> {
                        if (onDeny == null || onDeny.invoke())
                            m.delete().queue()
                        cancel()
                    }
                }
            }
        })
    }
}

fun convertSnowflake(snowflake: String): Date {
    val s = snowflake.toLong()
    val time = s.shr(22)
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = time + 1420070400000
    return calendar.time
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
    Model.first(me.mrkirby153.KirBot.database.models.Channel::class.java, this.id)?.run {
        this.hidden = this@hide.getPermissionOverride(this@hide.guild.publicRole)?.denied?.contains(
                Permission.MESSAGE_READ) ?: false
        this.save()
    }
}

fun TextChannel.unhide() {
    val public = this.getPermissionOverride(guild.publicRole) ?: return
    public.manager.clear(Permission.MESSAGE_READ).queue()
    Model.first(me.mrkirby153.KirBot.database.models.Channel::class.java, this.id)?.run {
        this.hidden = this@unhide.getPermissionOverride(
                this@unhide.guild.publicRole)?.denied?.contains(Permission.MESSAGE_READ) ?: false
        this.save()
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

fun String.urlEscape(): String {
    return this.replace(Regex("(<)(https?://\\S+)(>)"), "$2").replace(Regex("https?://\\S+"),
            "<$0>")
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

fun String.escapeMentions(): String {
    // Replace all @ symbols with @ followed by a Zero-Width Space
    return this.replace("@", "@\u200B")
}

val User.nameAndDiscrim
    get() = this.name + "#" + this.discriminator

fun Message.removeReaction(user: User, reaction: String) {
    Bot.LOG.debug("Removing $reaction from $id")
    this.reactions.filter {
        Bot.LOG.debug("Reaction: ${it.reactionEmote.name}")
        it.reactionEmote.name == reaction
    }.forEach {
        it.removeReaction(user).queue()
    }
}

fun Message.removeReactionById(user: User, reactionId: String) {
    this.reactions.filter { it.reactionEmote.isEmote }.filter { it.reactionEmote.id == reactionId }.forEach {
        it.removeReaction(user).queue()
    }
}
fun <T> JSONArray.toTypedArray(clazz: Class<T>): List<T> {
    return this.map { it as T }
}