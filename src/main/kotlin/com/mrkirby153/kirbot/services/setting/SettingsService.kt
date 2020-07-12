package com.mrkirby153.kirbot.services.setting

import net.dv8tion.jda.api.entities.Guild

/**
 * Service for handling guild settings
 */
interface SettingsService {

    /**
     * Gets a setting on the guild. If the setting does not existing the database, the [Setting]'s
     * default value ([Setting.default]) is returned instead
     *
     * @param setting The setting to retrieve on the guild
     * @param guild The guild to retrieve the setting on
     * @return The setting from the guild
     */
    fun <T> getSetting(setting: Setting<T>, guild: Guild): T?

    /**
     * Sets a setting on the guild
     *
     * @param setting The setting to set on the guild
     * @param guild The guild to set the setting on
     * @param value The value of the setting to set
     */
    fun <T> setSetting(setting: Setting<T>, guild: Guild, value: T?)
}