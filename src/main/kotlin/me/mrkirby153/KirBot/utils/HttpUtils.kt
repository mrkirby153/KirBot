package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.kcutils.child
import me.mrkirby153.kcutils.mkdirIfNotExist
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

object HttpUtils {

    val cacheDirectory: File = Bot.files.data.child("okhttp-cache").mkdirIfNotExist()

    val maxCacheSize: Long = 10 * 1024 * 1024 // 10 MiB

    val cache: Cache = Cache(cacheDirectory, maxCacheSize)

    val CLIENT: OkHttpClient = OkHttpClient.Builder().run {
        build()
    }

    fun clearCache(){
        Bot.LOG.debug("Cleared HTTP cache!")
        cacheDirectory.listFiles().forEach {
            it.deleteRecursively()
        }
    }
}