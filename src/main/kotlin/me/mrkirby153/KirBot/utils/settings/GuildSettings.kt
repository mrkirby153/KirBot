package me.mrkirby153.KirBot.utils.settings

import org.json.JSONArray
import org.json.JSONObject

object GuildSettings {

    val botNick = StringSetting("bot_nick")

    val commandPrefix = StringSetting("command_prefix", "!")
    val commandSilentFail = BooleanSetting("command_silent_fail", false)
    val commandWhitelistChannels = JsonArraySetting("cmd_whitelist", JSONArray())

    val musicEnabled = BooleanSetting("music_enabled", false)
    val musicSkipTimer = NumberSetting("music_skip_timer", 30)
    val musicChannels = JsonArraySetting("music_channels", JSONArray())
    val musicWhitelistMode = StringSetting("music_mode", "OFF")
    val musicMaxQueueLength = NumberSetting("music_max_queue_length", -1)
    val musicSkipCooldown = NumberSetting("music_skip_cooldown", 0)
    val musicMaxSongLength = NumberSetting("music_max_song_length", -1)
    val musicPlaylistsEnabled = BooleanSetting("music_playlists", false)

    val mutedRole = StringSetting("muted_role")
    val logTimezone = StringSetting("log_timezone", "UTC")

    val antiRaidCount = NumberSetting("anti_raid_count", 0)
    val antiRaidPeriod = NumberSetting("anti_raid_period", 0)
    val antiRaidEnabled = BooleanSetting("anti_raid_enabled", false)
    val antiRaidAction = StringSetting("anti_raid_action", "NONE")
    val antiRaidQuietPeriod = NumberSetting("anti_raid_quiet_period", 0)
    val antiRaidAlertRole = StringSetting("anti_raid_alert_role")
    val antiRaidAlertChannel = StringSetting("anti_raid_alert_channel")

    val logManualInfractions = BooleanSetting("log_manual_inf", false)

    val starboardChannel = StringSetting("starboard_channel_id")
    val starboardEnabled = BooleanSetting("starboard_enabled", false)
    val starboardSelfStar = BooleanSetting("starboard_self_star", false)
    val starboardStarCount = NumberSetting("starboard_star_count", 0)
    val starboardGildCount = NumberSetting("starboard_gild_count", 0)

    val spamSettings = JsonObjectSetting("spam_settings", JSONObject())
    val censorSettings = JsonObjectSetting("censor_settings", JSONObject())

    val userPersistence = NumberSetting("user_persistence", 0)
    val persistRoles = JsonArraySetting("persist_roles", JSONArray())
}