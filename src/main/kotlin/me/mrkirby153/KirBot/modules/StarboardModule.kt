package me.mrkirby153.KirBot.modules

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.DB
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.database.models.guild.GuildMessage
import me.mrkirby153.KirBot.database.models.guild.starboard.StarboardEntry
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.kirbotGuild
import me.mrkirby153.KirBot.utils.settings.GuildSettings
import me.mrkirby153.KirBot.utils.toTypedArray
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import org.json.JSONArray
import java.sql.Timestamp
import java.time.Instant


class StarboardModule : Module("starboard") {

    private val STAR = "⭐"
    private val GILD_STAR = "\uD83C\uDF1F"

    override fun onLoad() {

    }

    private fun getStarboardChannel(guild: Guild): TextChannel? {
        val get = GuildSettings.starboardChannel.nullableGet(guild) ?: return null
        return guild.getTextChannelById(get)
    }

    private fun incrementStar(mid: String) {
        DB.executeInsert(
                "INSERT INTO `starboard` (`id`, `star_count`, `created_at`, `updated_at`) VALUES (?, 1, ?, ?) ON DUPLICATE KEY UPDATE star_count = star_count + 1",
                mid, Timestamp.from(Instant.now()), Timestamp.from(Instant.now()))
    }

    private fun decrementStar(mid: String) {
        DB.executeUpdate("UPDATE `starboard` SET `star_count` = `star_count` - 1 WHERE `id` = ?",
                mid)
    }

    private fun getStarCount(mid: String) = DB.getFirstColumn<Long>(
            "SELECT `star_count` FROM `starboard` WHERE `id` = ?", mid)

    private fun getStarboardEntry(mid: String) = Model.query(StarboardEntry::class.java).where("id",
            mid).first()

    @Subscribe
    fun onReactionAdd(event: MessageReactionAddEvent) {
        if (event.reactionEmote.name == STAR) {
            if (!GuildSettings.starboardEnabled.get(event.guild))
                return
            debug("Incrementing star on ${event.messageId}")
            val msg = event.textChannel.retrieveMessageById(event.messageId).complete()
            if (event.channel.id == getStarboardChannel(event.guild)?.id)
                return
            if (msg.author.id in blocked(event.guild)) {
                debug("Author is blocked. Their messages can't be starred")
                return
            }
            if (event.user?.id in blocked(event.guild)) {
                debug("User is blocked. Their star has no effect")
                return
            }
            if (msg.author == event.user)
                if (GuildSettings.starboardSelfStar.get(event.guild))
                    incrementStar(event.messageId)
                else {
                    return
                }
            else
                incrementStar(event.messageId)
            updateStarboardMessage(event.guild, event.messageId)
        }
    }

    @Subscribe
    fun onReactionRemove(event: MessageReactionRemoveEvent) {
        if (event.reactionEmote.name == STAR) {
            if (!GuildSettings.starboardEnabled.get(event.guild))
                return
            debug("Decrementing star on ${event.messageId}")
            decrementStar(event.messageId)
            if (getStarboardEntry(event.messageId) != null)
                updateStarboardMessage(event.guild, event.messageId)
        }
    }

    @Subscribe
    fun onMessageDelete(event: MessageDeleteEvent) {
        if (!GuildSettings.starboardEnabled.get(event.guild))
            return
        val entry = getStarboardEntry(event.messageId) ?: return
        if (entry.starboardMid != null)
            getStarboardChannel(event.guild)?.deleteMessageById(entry.starboardMid!!)?.queue()
        entry.delete()
    }

    fun updateStarboardMessage(guild: Guild, mid: String) {
        var entry = getStarboardEntry(mid)
        val message = Model.where(GuildMessage::class.java, "id", mid).first() ?: return
        val apiMsg = Bot.shardManager.getGuildById(message.serverId)?.getTextChannelById(
                message.channel)?.retrieveMessageById(mid)?.complete()
        if (apiMsg != null) {
            // Update the star count
            val starCount = apiMsg.reactions.firstOrNull { it.reactionEmote.name == STAR }?.count?.toLong()
                    ?: 0
            if (starCount > 0) {
                // Create if it doesn't exist
                if (entry == null) {
                    entry = StarboardEntry()
                    entry.id = mid
                    entry.count = starCount
                    entry.save()
                } else {
                    entry.count = starCount
                    entry.save()
                }
            } else {
                entry?.delete()
            }
        }
        if (entry.hidden || apiMsg == null || entry.count < GuildSettings.starboardStarCount.get(
                        guild)) {
            // Delete the star from the board if it's hidden or the original message was deleted or it's fallen below the star count
            // TODO 12/19/2018 Make the latter configurable
            if (entry.starboardMid != null) {
                getStarboardChannel(guild)?.deleteMessageById(entry.starboardMid!!)?.queue()
                entry.starboardMid = null
                if (entry.count == 0L)
                    entry.delete()
                else
                    entry.save()
            }
            if (apiMsg == null)
                entry.delete()
            return
        }
        // Construct the star message
        val msg = MessageBuilder()
        msg.setContent(
                "${if (entry.count < GuildSettings.starboardGildCount.get(
                                guild)) STAR else GILD_STAR} ${entry.count} <#${apiMsg.channel.id}> $mid")
        msg.setEmbed(embed {
            description {
                +message.message
            }
            val att = message.attachments
            if (att != null) {
                image = att.split(",").first().trim()
            }
            timestamp {
                timestamp = apiMsg.timeCreated.toInstant()
            }
        }.build())
        val sbMessage = if (entry.starboardMid != null) getStarboardChannel(
                guild)?.retrieveMessageById(
                entry.starboardMid!!)?.complete() else null
        if (sbMessage != null) {
            sbMessage.editMessage(msg.build()).queue()
        } else {
            getStarboardChannel(guild)?.sendMessage(msg.build())?.queue {
                entry.starboardMid = it.id
                entry.save()
            }
        }
    }

    fun block(guild: Guild, user: User) {
        guild.kirbotGuild.runWithExtraData(true) {
            val blocked = it.optJSONArray("starboard_blocked")
            if (blocked == null) {
                it.put("starboard_blocked", JSONArray(arrayOf(user.id)))
            } else {
                if (!blocked.contains(user.id))
                    blocked.put(user.id)
            }
        }
    }

    fun unblock(guild: Guild, user: User) {
        guild.kirbotGuild.runWithExtraData(true) {
            val blocked = it.optJSONArray("starboard_blocked") ?: return@runWithExtraData
            val itr = blocked.iterator()
            while (itr.hasNext()) {
                if (itr.next() == user.id) {
                    itr.remove()
                }
            }
        }
    }

    private fun blocked(guild: Guild): List<String> {
        var blocked = emptyList<String>()
        guild.kirbotGuild.runWithExtraData {
            blocked = it.optJSONArray("starboard_blocked")?.toTypedArray(String::class.java)
                    ?: emptyList()
        }
        return blocked
    }
}