package me.mrkirby153.KirBot.command.processors

import me.mrkirby153.KirBot.command.MessageProcessor
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.MessageBuilder
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

class LaTeXProcessor : MessageProcessor("$$", "$$") {

    val preamble = "\\usepackage{amsmath}\n\\usepackage{amsfonts}\n\\usepackage{amssymb}"

    override fun process(context: Context) {
        val msg = context.send().text("Processing LaTeX....").submit(true)
        context.channel.sendTyping().submit(true)
        for (match in matches) {
            val url = getImageUrl(match)
            val u = URL(url)
            val con = u.openConnection() as HttpURLConnection
            con.setRequestProperty("User-Agent", "KirBot")

            val input = con.inputStream
            val m = MessageBuilder().apply {
                append(match)
            }
            context.channel.sendFile(input, "latex.png", m.build()).queue()
        }
        context.channel.deleteMessageById(msg.get().id).queue()
    }

    fun getImageUrl(latex: String): String? {
        val url = URL("http://quicklatex.com/latex3.f")
        val con = url.openConnection() as HttpURLConnection
        val postData = "formula=$latex&mode=0&out=1&fsize=45px&fcolor=737F8D&preamble=$preamble".toByteArray(Charset.defaultCharset())
        con.requestMethod = "POST"
        con.setRequestProperty("User-Agent", "KirBot")
        con.setRequestProperty("Content-Type", "application/x-ww-form-urlencoded")
        con.setRequestProperty("Content-Length", postData.size.toString())
        con.setRequestProperty("charset", "utf-8")

        con.doOutput = true

        val os = con.outputStream
        os.write(postData)
        os.flush()
        os.close()

        val respCode = con.responseCode

        if (respCode == HttpURLConnection.HTTP_OK) {
            val `in` = BufferedReader(InputStreamReader(con.inputStream))
            val resp = buildString {
                while (true) {
                    val value: String = `in`.readLine() ?: break
                    if (value.startsWith("http:"))
                        append(value)
                }
            }
            return resp.split(" ")[0]
        }
        return null
    }
}