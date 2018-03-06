package me.mrkirby153.KirBot.modules

import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.database.models.Quote
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.utils.GREEN_TICK
import me.mrkirby153.KirBot.utils.RED_TICK
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.KirBot.utils.removeReaction
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import java.awt.Color
import java.util.concurrent.TimeUnit

class Quotes : Module("quote") {

    var quoteReaction = "\uD83D\uDDE8"

    override fun onLoad() {
        log("Quote reaction is $quoteReaction")
        if (System.getProperty("kirbot.dev", "false").toBoolean()) {
            quoteReaction = "\uD83D\uDCA9"
            log("Dev environment, reaction is $quoteReaction")
        }
    }

    fun getBlockedUsers(guild: KirBotGuild): Array<String> {
        return guild.extraData.optJSONArray("quote-blocked")?.map { it.toString() }?.toTypedArray()
                ?: arrayOf()
    }

    fun blockUser(guild: KirBotGuild, user: String) {
        val array = guild.extraData.optJSONArray(
                "quote-blocked")?.map { it.toString() }?.toMutableList() ?: mutableListOf()
        if (user !in array)
            array.add(user)
        guild.extraData.put("quote-blocked", array)
        guild.saveData()
    }

    fun unblockUser(guild: KirBotGuild, user: String) {
        val array = guild.extraData.optJSONArray(
                "quote-blocked")?.map { it.toString() }?.toMutableList() ?: return
        array.remove(user)
        guild.extraData.put("quote-blocked", array)
        guild.saveData()
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.reactionEmote.name != quoteReaction)
            return
        debug("Beginning quote sequence")
        event.channel.getMessageById(event.messageId).queue { msg ->
            if (msg == null)
                return@queue
            if (msg.author == event.user) {
                debug("Denied quote, you can't quote yourself")
                event.channel.sendMessage(
                        "$RED_TICK ${event.user.asMention} You can't quote yourself!").queue { m ->
                    m.delete().queueAfter(30, TimeUnit.SECONDS) {
                        msg.removeReaction(event.user, quoteReaction)
                    }
                }
                return@queue
            }
            if (event.user.id in getBlockedUsers(event.guild.kirbotGuild)) {
                debug("Denied, user is blocked from quoting")
                msg.removeReaction(event.user, quoteReaction)
                return@queue
            }
            // Create the quote
            val q = Model.first(Quote::class.java, Pair("message_id", event.messageId))
            if (q != null) {
                debug("Denied, quote already exists")
                return@queue
            }

            val quote = Quote()
            quote.serverId = event.guild.id
            quote.messageId = event.messageId
            quote.user = msg.author.name
            quote.content = msg.contentDisplay
            quote.save()

            msg.addReaction(GREEN_TICK.emote).queue()

            event.channel.sendMessage(embed("Quote") {
                color = Color.BLUE
                author {
                    user(msg.author)
                }
                description {
                    +"A new quote has been created by `${event.user.nameAndDiscrim}`! \nType `${event.guild.kirbotGuild.settings.cmdDiscriminator}quote ${quote.id}` to retrive the quote"
                }
                fields {
                    field {
                        title = "ID"
                        inline = true
                        description = quote.id.toString()
                    }
                    field {
                        title = "message"
                        inline = true
                        description = quote.content
                    }
                }
                timestamp {
                    timestamp = msg.creationTime.toInstant()
                }
            }.build()).queue()
        }
    }
}