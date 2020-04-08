package me.mrkirby153.KirBot.modules.censor

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.logName
import me.mrkirby153.KirBot.utils.sanitize
import me.mrkirby153.KirBot.utils.toTypedArray
import me.mrkirby153.kcutils.utils.IdGenerator
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent

class NicknameCensor : Module("nick_censor") {

    private val idGenerator = IdGenerator(IdGenerator.ALPHANUMERIC)

    init {
        dependencies.add(Redis::class.java)
    }

    override fun onLoad() {
    }


    @Subscribe
    fun onNicknameChange(e: GuildMemberUpdateNicknameEvent) {
        tryCensor(e.member)
    }

    @Subscribe
    fun onUsernameChange(e: UserUpdateNameEvent) {
        e.jda.guilds.mapNotNull { it.getMember(e.user) }.forEach { tryCensor(it) }
    }

    @Subscribe
    fun onUserJoin(e: GuildMemberJoinEvent) {
        tryCensor(e.member)
    }

    private fun tryCensor(member: Member) {
        val rules = Censor.getEffectiveSettings(member.user, member.guild)
        rules.forEach { rule ->
            val tokens = rule.optJSONArray("blocked_nicks").toTypedArray(String::class.java)
            val matchedTokens = mutableListOf<String>()
            tokens.forEach { token ->
                if (token.startsWith("r:")) {
                    val r = Regex(token.substring(2))
                    if (r.containsMatchIn(member.effectiveName.toLowerCase())) {
                        matchedTokens.add(token)
                    }
                } else {
                    if (member.effectiveName.toLowerCase().contains(token.toLowerCase())) {
                        matchedTokens.add(token)
                    }
                }
            }
            if (matchedTokens.isNotEmpty()) {
                censor(member, matchedTokens.joinToString(", "))
            }
        }
    }

    private fun censor(member: Member, blacklistedToken: String) {
        // Check for our last censor attempt
        val redisKey = "nick_censor:${member.guild.id}:${member.user.id}"
        ModuleManager[Redis::class.java].getConnection().use {
            if (it.get(redisKey) != null) {
                Bot.LOG.debug("Ignoring censor for $member as they've been censored too frequently")
                return
            }
        }
        if (!member.guild.selfMember.hasPermission(Permission.NICKNAME_MANAGE)) {
            member.guild.kirbotGuild.logManager.genericLog(LogEvent.NAME_CENSOR, ":warning:",
                    "Could not rename ${member.user.logName.sanitize()} due to missing permissions")
        }
        val newName = "Censored Nickname ${idGenerator.generate()}"
        member.guild.modifyNickname(member, newName).queue {
            member.guild.kirbotGuild.logManager.genericLog(LogEvent.NAME_CENSOR,
                    ":no_entry_sign:",
                    "Renamed ${member.user.logName} to ${newName.sanitize()}. Blacklisted char sequence `$blacklistedToken`")
        }
        ModuleManager[Redis::class.java].getConnection().use {
            it.set(redisKey, System.currentTimeMillis().toString())
            it.expire(redisKey, 30)
        }
    }
}