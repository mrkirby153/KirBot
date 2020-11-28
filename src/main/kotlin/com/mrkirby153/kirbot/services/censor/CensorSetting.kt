package com.mrkirby153.kirbot.services.censor

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * DTO for the [CensorService] settings
 */
data class CensorSetting(
        @JsonProperty("_level")
        var level: Int,

        @JsonProperty("_id")
        var id: String,

        @JsonProperty("zalgo")
        var zalgo: Boolean,

        @JsonProperty("blocked_words")
        var blockedWords: List<String>,

        @JsonProperty("blocked_tokens")
        var blockedTokens: List<String>,

        @JsonProperty("invites")
        var invites: Invite,

        @JsonProperty("domains")
        var domains: Domains
) {

    /**
     * A [CensorSetting]'s invite setting
     */
    data class Invite(
            @JsonProperty("enabled")
            var enabled: Boolean,

            @JsonProperty("guild_whitelist")
            var whitelist: List<String>,

            @JsonProperty("guild_blacklist")
            var blacklist: List<String>
    )

    /**
     * A [CensorSetting]'s domain settings
     */
    data class Domains(
            @JsonProperty(value = "enabled")
            var enabled: Boolean,

            @JsonProperty(value = "whitelist")
            var whitelist: List<String>,

            @JsonProperty(value = "blacklist")
            var blacklist: List<String>
    )
}