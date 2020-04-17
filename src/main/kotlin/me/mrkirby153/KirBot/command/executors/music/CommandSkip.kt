package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.music.MusicModule
import me.mrkirby153.KirBot.modules.music.MusicModule.Companion.alone
import me.mrkirby153.KirBot.modules.music.MusicModule.Companion.isDJ
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.b
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.escapeMarkdown
import me.mrkirby153.KirBot.utils.settings.GuildSettings
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.Permission
import java.awt.Color
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class CommandSkip @Inject constructor(private val musicModule: MusicModule){
    private val skipCooldown = mutableMapOf<String, Long>()

    @Command(name = "skip", aliases = ["next"],
            permissions = [Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_EMBED_LINKS],
            category = CommandCategory.MUSIC)
    @CommandDescription("Skips the currently playing song")
    fun execute(context: Context, cmdContext: CommandContext) {
        if (!GuildSettings.musicEnabled.get(context.guild))
            return
        val manager = musicModule.getManager(context.guild)
        if (!manager.playing) {
            throw CommandException("I am not playing anything right now")
        }

        val skipIn = skipCooldown[context.author.id] ?: 0
        if (System.currentTimeMillis() < skipIn || skipIn == -1L) {
            throw CommandException(buildString {
                append("You are doing that too much\n\n")
                if (skipIn == -1L)
                    append("Try again after your current poll expires")
                else
                    append("Try again in ${Time.format(1, skipIn - System.currentTimeMillis())}")
            })
        }

        val currentlyPlaying = manager.nowPlaying ?: throw CommandException("Nothing playing!")
        val skipTimer = GuildSettings.musicSkipTimer.get(context.guild)
        if (alone(context.member)) {
            context.send().success("Playing next song").queue()
            manager.playNextTrack()
            return
        }
        context.send().embed("Music") {
            color = Color.GREEN
            description {
                +b(context.author.name)
                +" has voted to skip the current track: \n"
                +currentlyPlaying.info.title.escapeMarkdown() link currentlyPlaying.info.uri
                +"\nReact with :thumbsup: or :thumbsdown: to vote"
                +"\nWhichever has the most votes in ${Time.format(1, skipTimer * 1000L)} wins!"
            }
            timestamp {
                now()
            }
        }.rest().queue { m ->
            skipCooldown[context.author.id] = System.currentTimeMillis() + (skipTimer * 1000) + 1500
            val cd = GuildSettings.musicSkipCooldown.get(context.guild)
            if (cd > 0)
                skipCooldown[context.author.id] = (skipCooldown[context.author.id]
                        ?: 0) + (cd * 1000).toLong()
            m.addReaction("\uD83D\uDC4D").queue() // Thumbs up
            m.addReaction("\uD83D\uDC4E").queue() // Thumbs down
            m.editMessage(embed("Music") {
                description { +"Voting has ended. Check newer messages for results" }
                color = Color.RED
            }.build()).queueAfter(skipTimer.toLong(), TimeUnit.SECONDS) {
                if (manager.nowPlaying != currentlyPlaying) {
                    context.send().embed("Music") {
                        color = Color.CYAN
                        description { +"The song has changed, canceling vote" }
                        timestamp { now() }
                    }.rest().queue()
                    return@queueAfter
                }

                var skip = 0
                var stay = 0
                it.reactions.forEach {
                    when (it.reactionEmote.name) {
                        "\uD83D\uDC4D" -> skip = it.count - 1
                        "\uD83D\uDC4E" -> stay = it.count - 1
                    }
                }
                context.send().embed("Music") {
                    color = Color.CYAN
                    description {
                        if (skip > stay) {
                            appendln("The vote has passed. Playing the next song")
                            manager.playNextTrack()
                        } else {
                            appendln("The vote has failed.")
                        }
                    }
                    timestamp { now() }
                }.rest().queue()
            }
        }
    }

    @Command(name = "force", clearance = 0, parent = "skip", category = CommandCategory.MUSIC)
    fun forceSkip(context: Context, cmdContext: CommandContext) {
        if (!isDJ(context.member))
            throw CommandException("You must be a DJ to use this command! (Have a role named `DJ`)")
        context.channel.sendMessage("Skipping song").queue()
        musicModule.getManager(context.guild).playNextTrack()
    }
}