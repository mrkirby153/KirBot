package me.mrkirby153.KirBot.utils.embed

import net.dv8tion.jda.core.entities.MessageEmbed
import java.awt.Color


fun embed(title: String? = null, init: EmbedBuilder.() -> Unit): EmbedBuilder {
    val builder = EmbedBuilder()
    if (title != null)
        builder.title { +title }
    builder.init()
    return builder
}

@DslMarker
annotation class Marker

@Marker
class TextBuilder {
    private val builder = StringBuilder()

    val length: Int
        get() = builder.length

    fun appendln(string: String): TextBuilder {
        builder.append("$string\n")
        return this
    }

    fun append(string: String): TextBuilder {
        builder.append(string)
        return this
    }

    operator fun String.unaryPlus() {
        builder.append(this)
    }

    override fun toString(): String {
        return builder.toString()
    }
}

@Marker
class FieldBuilder {
    val fields = mutableListOf<Field>()

    fun field(blank: Boolean = false, init: (Field.() -> Unit)? = null): Field {
        val field = Field()
        field.blank = blank
        if (init != null) {
            field.init()
        }
        fields.add(field)
        return field
    }
}

@Marker
class Author {
    var name: String? = null
    var url: String? = null
    var iconUrl: String? = null
}

@Marker
class Field {
    var inline = false
    var title = ""
    var description = ""
    var blank = false

    fun description(init: TextBuilder.() -> Unit) {
        val builder = TextBuilder()
        builder.append(description)
        builder.init()
        description = builder.toString()
    }
}

@Marker
class Footer {
    private val text = TextBuilder()
    var url: String? = null

    fun text(init: TextBuilder.() -> Unit) {
        text.init()
    }

    fun getText(): TextBuilder {
        return this.text
    }
}

@Marker
open class EmbedBuilder {
    private val title = TextBuilder()
    private val description = TextBuilder()
    private val fields = FieldBuilder()
    private val author = Author()
    private val footer = Footer()

    var thumbnail: String? = null
    var color: Color? = null


    fun title(init: TextBuilder.() -> Unit): TextBuilder {
        title.init()
        return title
    }

    fun description(init: TextBuilder.() -> Unit) {
        description.init()
    }

    fun fields(init: FieldBuilder.() -> Unit) {
        fields.init()
    }

    fun author(init: Author.() -> Unit) {
        author.init()
    }

    fun footer(init: Footer.() -> Unit) {
        footer.init()
    }

    fun build(): MessageEmbed {
        val embed = net.dv8tion.jda.core.EmbedBuilder()
        if (title.toString().isNotEmpty())
            embed.setTitle(title.toString())
        embed.setDescription(description.toString())
        fields.fields.forEach { field ->
            if (field.blank) {
                embed.addBlankField(field.inline)
                return@forEach
            }
            embed.addField(field.title, field.description, field.inline)
        }
        embed.setThumbnail(thumbnail)
        embed.setColor(color)
        embed.setAuthor(author.name, author.url, author.iconUrl)
        embed.setFooter(footer.getText().toString(), footer.url)
        return embed.build()
    }
}