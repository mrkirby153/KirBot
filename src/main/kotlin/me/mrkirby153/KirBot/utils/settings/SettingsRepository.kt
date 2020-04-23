package me.mrkirby153.KirBot.utils.settings

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.common.util.concurrent.UncheckedExecutionException
import com.mrkirby153.bfs.Pair
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.GuildSetting
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.sharding.ShardManager

object SettingsRepository {

    val settingsCache: Cache<String, String?> = CacheBuilder.newBuilder().maximumSize(
            10000).build<String, String?>()

    private val settingsListeners = mutableMapOf<String, MutableList<(Guild, String?) -> Unit>>()

    fun get(guildId: String, key: String): String? {
        try {
            return settingsCache.get("$guildId-$key") loader@{
                Bot.LOG.debug("Retrieving $key from the database for $guildId")
                val setting = Model.query(GuildSetting::class.java).where("guild", guildId).where(
                        "key",
                        key).first()
                if (setting != null) {
                    Bot.LOG.debug("Retrieved \"${setting.value}\" from the database")
                } else {
                    Bot.LOG.debug("Retrieved \"null\" from the database")
                    throw NoSuchElementException("Null")
                }
                return@loader setting.value
            }
        } catch (e: UncheckedExecutionException) {
            if (e.cause is NoSuchElementException) {
                return null // The value does not exist in the database
            } else {
                throw e
            }
        }
    }

    fun set(guildId: String, key: String, value: String?) {
        if (value == null) {
            Model.query(GuildSetting::class.java).where("guild", guildId).where("key", key).delete()
        } else {
            val exists = Model.query(GuildSetting::class.java).where("guild", guildId).where("key",
                    key).exists()
            if (exists) {
                Model.query(GuildSetting::class.java).where("guild", guildId).where("key",
                        key).update(mutableListOf(Pair<String, Any>("one", "two")))
            } else {
                val gs = GuildSetting()
                gs.guildId = guildId
                gs.key = key
                gs.value = value
                gs.save()
            }
        }
        if (value != null)
            settingsCache.put("$guildId-$key", value)
        else
            settingsCache.invalidate("$guildId-$key")
    }

    fun onSettingsChange(guildId: String, key: String, newValue: String?) {
        Bot.LOG.debug("$key on $guildId changed to \"$newValue\"")
        if (newValue != null)
            settingsCache.put("$guildId-$key", newValue) // Update our cached value
        else
            settingsCache.invalidate("$guildId-$key")
        val guild = Bot.applicationContext.get(ShardManager::class.java).getGuildById(guildId) ?: return
        settingsListeners[key]?.forEach {
            try {
                it.invoke(guild, newValue)
            } catch (e: Exception) {
                Bot.LOG.error("An error occurred when invoking a settings change listener", e)
            }
        }
    }

    fun registerSettingsListener(key: String, listener: (Guild, String?) -> Unit) {
        settingsListeners.getOrPut(key) { mutableListOf() }.add(listener)
    }
}