package me.mrkirby153.KirBot.utils

import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.GuildSetting
import net.dv8tion.jda.api.entities.Guild
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

object SettingsRepository {

    private val settingsRepo = mutableMapOf<String, MutableMap<String, GuildSetting>>()

    private val settingListeners = mutableMapOf<String, MutableList<(Guild, String?) -> Unit>>()

    fun getAsJsonArray(guild: Guild, key: String, default: JSONArray? = null,
                       create: Boolean = false): JSONArray? {
        val json = get(guild, key, default?.toString(), create)
        return if (json != null) {
            JSONArray(JSONTokener(json))
        } else {
            null
        }
    }

    fun getAsJsonObject(guild: Guild, key: String, default: JSONObject? = null,
                        create: Boolean = false): JSONObject? {
        val json = get(guild, key, default?.toString(), create)
        return if (json != null) {
            JSONObject(JSONTokener(json))
        } else {
            null
        }
    }

    fun get(guild: Guild, key: String, default: String? = null, create: Boolean = false): String? {
        val cached = getCachedSetting(guild, key)
        if (cached != null) {
            return cached.value
        }

        val dbSettings = Model.query(GuildSetting::class.java).where("guild", guild.id).where("key",
                key).first()
        if (dbSettings != null) {
            // Cache the value retrieved from the DB
            settingsRepo.computeIfAbsent(guild.id) { mutableMapOf() }[key] = dbSettings
        }
        val dbVal = dbSettings?.value
        if (dbVal == null) {
            if (create) {
                val gs = GuildSetting()
                gs.guildId = guild.id
                gs.key = key
                gs.value = default
                gs.save()
                return default
            }
        }
        return dbVal ?: default
    }

    fun set(guild: Guild, key: String, value: String?) {
        val existing = Model.query(GuildSetting::class.java).where("guild", guild.id).where("key",
                key).first()
        // If we're setting it to null, delete it
        if (existing != null && value == null) {
            existing.delete()
            return
        }
        if (existing == null) {
            val newSettings = GuildSetting()
            newSettings.guildId = guild.id
            newSettings.key = key
            newSettings.value = value
            newSettings.save()
        } else {
            existing.value = value
            existing.save()
        }
    }

    fun broadcastSettingChange(guild: Guild, setting: String, newVal: String?) {
        Bot.LOG.debug("$setting on $guild changed to $newVal")
        if (newVal == null) {
            // Delete the cached settings
            val cached = getCachedSetting(guild, setting)
            if (cached != null) {
                this.settingsRepo[guild.id]?.remove(setting)
            }
        } else {
            // Update the cached setting
            val cached = getCachedSetting(guild, setting)
            cached?.value = newVal
        }
        this.settingListeners[setting]?.forEach {
            it.invoke(guild, newVal)
        }
    }

    fun registerSettingListener(key: String, listener: (Guild, String?) -> Unit) {
        this.settingListeners.getOrPut(key) { mutableListOf() }.add(listener)
    }

    private fun getCachedSetting(guild: Guild, key: String): GuildSetting? {
        // Check the settings repo
        val cached = this.settingsRepo[guild.id]
        return cached?.get(key)
    }
}