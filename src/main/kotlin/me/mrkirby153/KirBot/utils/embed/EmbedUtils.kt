package me.mrkirby153.KirBot.utils.embed

import net.dv8tion.jda.core.EmbedBuilder

@JvmOverloads
fun embed(title: String? = null): EmbedMaker = EmbedMaker().apply {
    setTitle(title)
}

inline fun embed(title: String? = null, value: EmbedMaker.() -> Unit): EmbedMaker {
    return embed(title).apply(value)
}

inline fun EmbedBuilder.description(value: () -> Any?): EmbedBuilder {
    setDescription(value().toString())
    return this
}

/** Bold strings.*/
inline fun b(string: String) = "**$string**"

/** Bold strings. */
inline fun b(any: Any?) = b(any.toString())

/** Italicize strings. */
inline fun i(string: String) = "*$string*"

/** Italicize strings. */
inline fun i(any: Any?) = i(any.toString())

/** Underline strings. */
inline fun u(string: String) = "__${string}__"

/** Underline strings. */
inline fun u(any: Any?) = u(any.toString())

/** Link strings to a URL. */
inline infix fun String.link(url: String) = "[$this]${if (true) "($url)" else "()"}"

/** Link strings to a URL. */
inline infix fun Any?.link(url: String) = toString() link url

inline fun code(language: String = "", code: () -> String) = "```$language\n${code()}```"

inline fun inlineCode(code: () -> String) = "`${code()}`"