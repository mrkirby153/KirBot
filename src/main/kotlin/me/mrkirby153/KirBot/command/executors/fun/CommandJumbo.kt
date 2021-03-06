package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.EMOJI_RE
import me.mrkirby153.KirBot.utils.HttpUtils
import net.dv8tion.jda.api.Permission
import okhttp3.Request
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


class CommandJumbo {

    @Command(name = "jumbo", arguments = ["<emojis:string...>"], category = CommandCategory.FUN,
            permissions = [Permission.MESSAGE_ATTACH_FILES])
    @CommandDescription("Sends a bigger version of the given emojis")
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val emojis = cmdContext.get<String>("emojis")!!

        val urls = mutableListOf<String>()

        emojis.split(" ").forEach { e ->
            val matcher = EMOJI_RE.toPattern().matcher(e)
            if (matcher.find()) {
                val id = matcher.group(2)
                urls.add("https://discordapp.com/api/emojis/$id.png")
            } else {
                urls.add(getEmojiUrl(e))
            }
        }

        var img: BufferedImage? = null
        urls.forEach { e ->
            val request = Request.Builder().url(e).build()
            val resp = HttpUtils.CLIENT.newCall(request).execute()
            val r = ImageIO.read(resp.body()?.byteStream())
            if (img == null)
                img = r
            else
                img = joinBufferedImage(img!!, r)
        }

        val finalImg = img ?: return

        val os = ByteArrayOutputStream()
        ImageIO.write(finalImg, "png", os)
        val `is` = ByteArrayInputStream(os.toByteArray())
        context.channel.sendFile(`is`, "emoji.png").queue {
            os.close()
            `is`.close()
        }
    }

    private fun getEmojiUrl(emoji: String): String {
        val first = String.format("%04X", emoji.codePoints().findFirst().asInt)
        return "https://twemoji.maxcdn.com/2/72x72/${first.toLowerCase()}.png"
    }

    private fun joinBufferedImage(img1: BufferedImage, img2: BufferedImage): BufferedImage {
        val offset = 5
        val width = img1.width + img2.width + offset
        val height = Math.max(img1.height, img2.height) + offset

        val newImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        val g2 = newImage.graphics
        g2.drawImage(img1, 0, 0, null)
        g2.drawImage(img2, img1.width + offset, 0, null)
        g2.dispose()
        return newImage
    }

    fun resizeImage(img: BufferedImage, width: Int, height: Int): BufferedImage {
        val tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH)
        val dImg = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        val g2d = dImg.createGraphics()
        g2d.drawImage(tmp, 0, 0, null)
        g2d.dispose()
        return dImg
    }
}