package me.mrkirby153.KirBot

import me.mrkirby153.KirBot.utils.child
import me.mrkirby153.KirBot.utils.createFileIfNotExist
import java.io.File

class BotFiles {

    val data = File("data").apply { if (!exists()) mkdir() }

    val properties = data.child("config.properties").createFileIfNotExist()

    val admins = data.child("admins").createFileIfNotExist()
}