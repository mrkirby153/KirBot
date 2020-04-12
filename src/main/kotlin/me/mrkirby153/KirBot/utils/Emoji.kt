package me.mrkirby153.KirBot.utils

const val RED_CROSS = "\u274C"
const val GREEN_CHECK = "\u2705"
const val NO_ENTRY = ":no_entry_sign:"


// Custom Emoji
val STATUS_ONLINE = CustomEmoji("status_online", "414874407429996555")
val STATUS_AWAY = CustomEmoji("status_away", "414874405844418560")
val STATUS_DND = CustomEmoji("status_dnd", "414874407044120586")
val STATUS_OFFLINE = CustomEmoji("status_offline", "414874407022886913")

val RED_TICK = CustomEmoji("kbRedTick", "414875062336880640")
val GREEN_TICK = CustomEmoji("kbGreenTick", "414875062001205249")

// Badge emoji
val VERIFIED_DEVELOPER = CustomEmoji("verifiedDeveloper", "698751571030442035")
val PARTNER = CustomEmoji("partner", "698752296582119434")
val HYPESQUAD_EVENTS = CustomEmoji("hse", "698752296556822528")
val BUG_HUNTER_TIER_2 = CustomEmoji("bug_hunter_tier_2", "698751916942950492")
val BUG_HUNTER = CustomEmoji("bug_hunter", "698751917504987207")
val HS_BRILLIANCE = CustomEmoji("brilliance", "698752296405827636")
val HS_BRAVERY = CustomEmoji("bravery", "698752296431124562")
val HS_BALANCE = CustomEmoji("balance", "698752296338849793")
val EARLY_SUPPORTER = CustomEmoji("early_supporter", "698755414095036486")
val STAFF = CustomEmoji("staff", "698755826466291742")

val EMOJI_RE = Regex("<a?:([^:]*):(\\d{17,18})>")