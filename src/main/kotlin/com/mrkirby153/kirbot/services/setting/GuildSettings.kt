package com.mrkirby153.kirbot.services.setting

import com.mrkirby153.kirbot.services.censor.CensorSetting

/**
 * Object storing all of the guild settings and their default values. These values can be retrieved
 * from the guild via the [SettingsService]
 */
object GuildSettings {

    /**
     * The guild's command prefix
     */
    val commandPrefix = StringSetting("cmd_prefix", "!")

    /**
     * Controls if commands should silently fail
     */
    val commandSilentFail = BooleanSetting("command_silent_fail", false)

    /**
     * The muted role on the server
     */
    val mutedRole = StringSetting("muted_role")

    /**
     * The configured censor settings on a server
     */
    val censorSettings = ArraySetting("censor_settings", CensorSetting::class.java, emptyArray())
}