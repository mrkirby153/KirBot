package com.mrkirby153.kirbot.services.setting

import com.mrkirby153.kirbot.entity.GuildSetting
import com.mrkirby153.kirbot.entity.repo.GuildSettingRepository
import net.dv8tion.jda.api.entities.Guild
import org.apache.logging.log4j.LogManager
import org.springframework.stereotype.Service

@Service
class SettingManager(private val settingRepository: GuildSettingRepository) : SettingsService {

    private val log = LogManager.getLogger()

    override fun <T> getSetting(setting: Setting<T>, guild: Guild): T? {
        log.debug("Retrieving {} for {}", setting.key, guild)
        // TODO 7/11/20 Cache this to take strain off the database
        val optional = settingRepository.getByGuildAndKey(guild.id, setting.key)
        return if (optional.isPresent) {
            setting.deserialize(optional.get().value)
        } else {
            setting.default
        }
    }

    override fun <T> setSetting(setting: Setting<T>, guild: Guild, value: T?) {
        log.debug("Updating {} on {} to {}", setting.key, guild, value)
        if (value == setting.default) {
            log.debug("Deleting key {} on {}", setting.key, guild)
            settingRepository.deleteByGuildAndKey(guild.id, setting.key)
        } else {
            val toSet = value?.run { setting.serialize(this) }
            settingRepository.getByGuildAndKey(guild.id, setting.key).ifPresentOrElse({ existing ->
                if (toSet == null) {
                    log.debug("Deleting key {} on {}", setting.key, guild)
                    settingRepository.delete(existing)
                } else {
                    log.debug("Updating key {} on {} to {}", setting.key, guild, value)
                    existing.value = toSet
                    settingRepository.save(existing)
                }
            }, {
                if (toSet == null) {
                    return@ifPresentOrElse
                }
                log.debug("Creating key {} on {}", setting.key, guild)
                val toSave = GuildSetting("${guild.id}_${setting.key}", guild.id, setting.key,
                        toSet)
                settingRepository.save(toSave)
            })
        }
    }

}