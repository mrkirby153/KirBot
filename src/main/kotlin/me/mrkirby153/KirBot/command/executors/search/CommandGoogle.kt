package me.mrkirby153.KirBot.command.executors.search

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.b
import me.mrkirby153.KirBot.utils.embed.link
import org.jsoup.Jsoup
import java.awt.Color
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Command(name = "google", aliases = arrayOf("g"), description = " Google search!", category = "Fun")
class CommandGoogle : CommandExecutor() {

    override fun execute(context: Context, args: Array<String>) {
        if (args.isEmpty()) {
            context.send().error("Please specify something to google!")
            return
        }
        try {
            context.channel.sendTyping().queue {
                val query = args.joinToString(" ")
                val blocks = Jsoup.connect("https://www.google.com/search?q=${URLEncoder.encode(query, StandardCharsets.UTF_8.displayName())}")
                        .userAgent("KirBot")
                        .get()
                        .select(".g")

                if (blocks.isEmpty()) {
                    context.send().error("No search results for `$query`").queue()
                    return@queue
                }

                context.send().embed {
                    setColor(Color.GREEN)
                    setAuthor("Google Results", "https://www.google.com", "https://www.google.com/favicon.ico")

                    description {
                        var count = 0

                        buildString {
                            for (block in blocks) {
                                if (count >= 3) break

                                val list = block.select(".r>a")
                                if (list.isEmpty()) continue

                                val entry = list[0]
                                val title = entry.text()
                                val url = entry.absUrl("href").replace(")", "\\)")
                                var desc: String? = null

                                val st = block.select(".st")
                                if (!st.isEmpty()) desc = st[0].text()

                                appendln(b(title link url)).appendln(desc)
                                count++
                            }
                        }
                    }
                }.rest().queue()
            }
        } catch(e: IOException) {
            context.send().error("Caught an exception while googling").queue()
            e.printStackTrace()
        }
    }
}