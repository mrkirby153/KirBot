package me.mrkirby153.KirBot.utils

import me.mrkirby153.KirBot.Bot
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File

object HttpUtils {

    val cacheDirectory: File = Bot.files.data.child("okhttp-cache").mkdirIfNotExist()

    val maxCacheSize: Long = 10 * 1024 * 1024 // 10 MiB

    val cache: Cache = Cache(cacheDirectory, maxCacheSize)

    val CLIENT: OkHttpClient = OkHttpClient.Builder().run {
        cache(cache)
        build()
    }

    fun clearCache(){
        Bot.LOG.info("Cleared HTTP cache!")
        cacheDirectory.listFiles().forEach {
            it.deleteRecursively()
        }
    }
}