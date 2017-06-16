package me.mrkirby153.KirBot.utils.embed

import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.entities.MessageEmbed
import java.awt.Color
import java.time.temporal.TemporalAccessor

@Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
open class EmbedProxy<T: EmbedProxy<T>> : EmbedBuilder() {
    inline fun field(inline: Boolean = false) = addBlankField(inline)

    inline fun field(name: String?, inline: Boolean = false, value: Any?): T {
        super.addField(name, value.toString(), inline)
        return this as T
    }

    inline fun field(name: String?, inline: Boolean = false, value: () -> Any?): T {
        super.addField(name, value().toString(), inline)
        return this as T
    }

    inline fun description(value: () -> Any?): T {
        setDescription(value().toString())
        return this as T
    }

    override fun setImage(url: String?): T {
        super.setImage(url)
        return this as T
    }

    override fun clearFields(): T {
        super.clearFields()
        return this as T
    }

    override fun appendDescription(description: CharSequence?): T {
        super.appendDescription(description)
        return this as T
    }

    override fun setAuthor(name: String?, url: String?, iconUrl: String?): T {
        super.setAuthor(name, url, iconUrl)
        return this as T
    }

    override fun setTimestamp(temporal: TemporalAccessor?): T {
        super.setTimestamp(temporal)
        return this as T
    }

    override fun setDescription(description: CharSequence?): T {
        super.setDescription(description)
        return this as T
    }

    override fun setColor(color: Color?): T {
        super.setColor(color)
        return this as T
    }

    override fun setFooter(text: String?, iconUrl: String?): T {
        super.setFooter(text, iconUrl)
        return this as T
    }

    override fun setTitle(title: String?): T {
        super.setTitle(title)
        return this as T
    }

    override fun setTitle(title: String?, url: String?): T {
        super.setTitle(title, url)
        return this as T
    }

    override fun setThumbnail(url: String?): T {
        super.setThumbnail(url)
        return this as T
    }

    override fun addField(field: MessageEmbed.Field?): T {
        super.addField(field)
        return this as T
    }

    override fun addField(name: String?, value: String?, inline: Boolean): T {
        super.addField(name, value, inline)
        return this as T
    }

    override fun addBlankField(inline: Boolean): T {
        super.addBlankField(inline)
        return this as T
    }
}