package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.MessageEmbed
import net.dv8tion.jda.core.entities.User
import java.awt.Color
import java.io.File
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
    // TODO 1/21/2017 Add support for "Bot Manager" role
    return Clearance.USER
}

@JvmOverloads
fun makeEmbed(title: String?, msg: String?, color: Color? = Color.WHITE, img: String? = null, thumb: String? = null, author: User? = null): MessageEmbed {
    return EmbedBuilder().run {
        setDescription(msg)
        setTitle(title)
        setColor(color)

        if (author != null) {
            setAuthor(author.name, null, author.avatarUrl)
        }
        setThumbnail(thumb)
        setImage(img)
        build()
    }
}