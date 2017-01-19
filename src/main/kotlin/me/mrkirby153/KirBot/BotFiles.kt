package me.mrkirby153.KirBot

import me.mrkirby153.KirBot.utils.child
import java.io.File

class BotFiles {

    val data = File("data").apply { if (!exists()) mkdir() }

    val properties = data.child("config.properties").apply { if (!exists()) createNewFile() }
}