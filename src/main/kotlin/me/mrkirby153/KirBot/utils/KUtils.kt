package me.mrkirby153.KirBot.utils

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