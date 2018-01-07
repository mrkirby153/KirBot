package me.mrkirby153.KirBot.logger

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.embed.b
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.settings
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.awt.Color

class LogManager(private val guild: Guild) {

    val logChannel: TextChannel?
        get() {
            val chanId = guild.settings().logChannel ?: return null
            if(chanId.isEmpty())
                return null
            return guild.getTextChannelById(chanId)
        }

    fun logMessageDelete(id: String) {
        Bot.messageDataStore.getMessageContent(id, { msg ->
            if (msg == null)
                return@getMessageContent
            if (msg.id == "-1")
                return@getMessageContent

            val author = Bot.shardManager.getUser(msg.author) ?: return@getMessageContent
            val chan = guild.getTextChannelById(msg.channel) ?: return@getMessageContent

            if (author.isBot)
                return@getMessageContent

            logChannel?.sendMessage(embed("Message Deleted") {
                color = Color.RED
                author {
                    user(author)
                }
                description {
                    +"Message deleted in ${chan.asMention}"
                    +"\n\n"
                    +msg.message
                }
                timestamp {
                    now()
                }
            }.build())?.queue()
        })
    }

    fun logBulkDelete(chan: TextChannel, count: Int) {
        logChannel?.sendMessage(embed("Bulk Delete") {
            color = Color.RED
            description {
                +"$count messages were deleted from ${chan.asMention}"
            }
            timestamp {
                now()
            }
        }.build())?.queue()
    }

    fun logEdit(message: Message) {
        Bot.messageDataStore.getMessageContent(message.id, { old ->
            if (old == null)
                return@getMessageContent
            val user = message.author
            if (user.isBot)
                return@getMessageContent
            if (old.message.equals(message.contentDisplay, true)) {
                return@getMessageContent
            }

            logChannel?.sendMessage(embed("Message Edit") {
                color = Color.CYAN
                author {
                    user(user)
                }
                description {
                    +"Message edited in ${message.textChannel.asMention}"
                    +"\n\n"
                    +(b("Old: "))
                    +old.message
                    +"\n"
                    +(b("New: "))
                    +message.contentDisplay
                }
                timestamp {
                    now()
                }
            }.build())?.queue()
        })
    }

    fun genericLog(title: String, description: String, color: Color? = Color.BLUE,
                   user: User? = null) {
        logChannel?.sendMessage(embed(title) {
            this.color = color ?: Color.BLUE
            description {
                +description
            }
            if (user != null) {
                author {
                    user(user)
                }
            }
            timestamp {
                now()
            }
        }.build())?.queue()
    }
}