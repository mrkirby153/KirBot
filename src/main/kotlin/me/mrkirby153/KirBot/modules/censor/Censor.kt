package me.mrkirby153.KirBot.modules.censor

import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.modules.censor.rules.CensorRule
import me.mrkirby153.KirBot.modules.censor.rules.DomainRule
import me.mrkirby153.KirBot.modules.censor.rules.InviteRule
import me.mrkirby153.KirBot.modules.censor.rules.TokenRule
import me.mrkirby153.KirBot.modules.censor.rules.ViolationException
import me.mrkirby153.KirBot.modules.censor.rules.WordRule
import me.mrkirby153.KirBot.modules.censor.rules.ZalgoRule
import me.mrkirby153.KirBot.utils.SettingsRepository
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.isNumber
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent
import org.json.JSONObject

class Censor : Module("censor") {

    private val censorRules = mutableListOf<CensorRule>()

    private val invitePattern = "(discord\\.gg|discordapp.com/invite)/([A-Za-z0-9\\-]+)"
    private val inviteRegex = Regex(invitePattern)


    override fun onLoad() {
        censorRules.clear()
        censorRules.add(ZalgoRule())
        censorRules.add(TokenRule())
        censorRules.add(WordRule())
        censorRules.add(InviteRule())
        censorRules.add(DomainRule())
    }

    @Subscribe
    fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author == event.jda.selfUser)
            return
        process(event.message)
    }

    @Subscribe
     fun onGuildMessageUpdate(event: GuildMessageUpdateEvent) {
        if (event.author == event.jda.selfUser)
            return
        process(event.message)
    }

    private fun process(message: Message) {
        try{
            val settings = getEffectiveSettings(message.author, message.guild)
            settings.forEach { setting ->
                this.censorRules.forEach { rule ->
                    rule.check(message, setting)
                }
            }
        } catch(exception : ViolationException) {
            message.guild.kirbotGuild.logManager.genericLog(LogEvent.MESSAGE_CENSOR, ":no_entry_sign:",
                    "Message (`${message.id}`) by **${message.author.nameAndDiscrim}** (`${message.author.id}`) censored in #${message.channel.name}: ${exception.msg} ```${message.contentRaw}```")
            message.delete().queue()
        }
    }

    private fun getSettings(guild: Guild): JSONObject {
        return SettingsRepository.getAsJsonObject(guild, "censor_settings", JSONObject(), true)!!
    }

    private fun getEffectiveSettings(user: User, guild: Guild): Array<JSONObject> {
        val clearance = user.getClearance(guild)

        val settings = getSettings(guild)

        val effectiveCats = settings.keySet().filter{ it.isNumber() }.map { it.toInt() }.filter { it >= clearance }

        val rules = mutableListOf<JSONObject>()

        effectiveCats.forEach { cat ->
            rules.add(settings.getJSONObject(cat.toString()))
        }
        return rules.toTypedArray()
    }
}