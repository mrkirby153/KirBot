package me.mrkirby153.KirBot.utils

import java.io.File
import java.util.*

fun File.child(path: String) = File(this, path)

fun File.readProperties(): Properties {
    return Properties().apply { load(this@readProperties.inputStream()) }
}