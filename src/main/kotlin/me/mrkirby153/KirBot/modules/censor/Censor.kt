package me.mrkirby153.KirBot.modules.censor

import com.google.common.cache.CacheBuilder
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.database.models.guild.CensorSettings
import me.mrkirby153.KirBot.logger.LogEvent
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.modules.censor.rules.CensorRule
import me.mrkirby153.KirBot.modules.censor.rules.DomainRule
import me.mrkirby153.KirBot.modules.censor.rules.InviteRule
import me.mrkirby153.KirBot.modules.censor.rules.TokenRule
import me.mrkirby153.KirBot.modules.censor.rules.ViolationException
import me.mrkirby153.KirBot.modules.censor.rules.WordRule
import me.mrkirby153.KirBot.modules.censor.rules.ZalgoRule
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class Censor : Module("censor") {

    private val censorRules = mutableListOf<CensorRule>()

    private val invitePattern = "(discord\\.gg|discordapp.com/invite)/([A-Za-z0-9\\-]+)"
    private val inviteRegex = Regex(invitePattern)

    private val cache = CacheBuilder.newBuilder().expireAfterWrite(2,
            TimeUnit.SECONDS).build<String, CensorSettings>()


    override fun onLoad() {
        censorRules.clear()
        censorRules.add(ZalgoRule())
        censorRules.add(TokenRule())
        censorRules.add(WordRule())
        censorRules.add(InviteRule())
        censorRules.add(DomainRule())
    }

    @SubscribeEvent
    fun onGuildMessageReceived(event: GuildMessageReceivedEvent) {
        if (event.author == event.jda.selfUser)
            return
        process(event.message)
    }

    @SubscribeEvent
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

    private fun getSettings(guild: Guild): CensorSettings {
        val settings = cache.getIfPresent(guild.id)
        return if (settings == null) {
            val s = Model.where(CensorSettings::class.java, "id", guild.id).first()
                    ?: CensorSettings().apply { this.id = guild.id }
            s
        } else {
            settings
        }
    }

    private fun getEffectiveSettings(user: User, guild: Guild): Array<JSONObject> {
        val clearance = user.getClearance(guild)

        val settings = getSettings(guild).settings

        val effectiveCats = settings.keySet().map { it.toInt() }.filter { it >= clearance }

        val rules = mutableListOf<JSONObject>()

        effectiveCats.forEach { cat ->
            rules.add(settings.getJSONObject(cat.toString()))
        }
        return rules.toTypedArray()
    }
}