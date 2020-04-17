package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Scheduler
import me.mrkirby153.KirBot.scheduler.Schedulable
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.b
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.escapeMarkdown
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.api.Permission
import java.awt.Color
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.inject.Inject


class CommandPoll @Inject constructor(private val scheduler: Scheduler){

    @Command(name = "poll",
            arguments = ["<duration:string>", "<question:string>", "<options:string...>"],
            category = CommandCategory.FUN,
            permissions = [Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_ADD_REACTION])
    @CommandDescription("Create polls")
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val timeString = cmdContext.get<String>("duration") ?: "0s"
        val duration = try {
            Time.parse(timeString)
        } catch (e: IllegalArgumentException) {
            throw CommandException("Invalid time string `$timeString`")
        }

        val endsAt = System.currentTimeMillis() + duration
        if (duration <= 0) {
            throw CommandException("Please specify a duration greater than zero!")
        }

        val question = cmdContext.get<String>("question")

        val options = (cmdContext.get<String>("options") ?: "").split(
                Regex("[,\\|]")).map { it.trim() }.filter { it.isNotBlank() }

        if (options.size <= 1) {
            throw CommandException("Please provide more than one option for the poll")
        }

        if (options.size > 9) {
            throw CommandException("You can only have 9 options for the poll!")
        }

        val filteredOptions = options.filter { it.isNotEmpty() }
        context.send().embed("Poll") {
            color = Color.GREEN
            description {
                +"Vote by clicking the reactions on the choices below! Results will be final in ${b(
                        Time.format(1, duration))}"
            }
            fields {
                if (question != null)
                    field {
                        title = "Question"
                        description = question
                    }
                field {
                    title = "Options"
                    description {
                        filteredOptions.forEachIndexed { index, option ->
                            appendln("${'\u0030' + (index)}\u20E3 **${option.escapeMarkdown()}**")
                        }
                    }
                }
            }
            footer {
                url = context.author.effectiveAvatarUrl
                text { +"Requested by ${context.author.nameAndDiscrim} - Ends" }
            }
            timestamp {
                millis(endsAt)
            }
        }.rest().queue {
            for (i in options.indices) {
                it.addReaction("${'\u0030' + i}\u20E3").queue()
            }
            val task = PollTask(context.guild.id, context.channel.id, it.id,
                    context.author.effectiveAvatarUrl, context.author.nameAndDiscrim, endsAt,
                    filteredOptions.toTypedArray(), question)
            scheduler.submit(task, duration, TimeUnit.MILLISECONDS)
        }
    }

    class PollTask(var guildId: String, var channelId: String, var messageId: String,
                   var avatarUrl: String, var nameAndDiscrim: String, var endsAt: Long,
                   var options: Array<String>, var question: String?) : Schedulable {
        override fun run() {
            val guild = Bot.shardManager.getGuildById(this.guildId) ?: return
            val channel = guild.getTextChannelById(channelId) ?: return
            val message = channel.retrieveMessageById(messageId).complete() ?: return
            message.editMessage(embed("Poll") {
                description {
                    +"Voting has ended, check newer messages for results"
                }
                footer {
                    url = avatarUrl
                    text { +"Requested by $nameAndDiscrim - Ended" }
                }
                fields {
                    field {
                        title = "Options"
                        description {
                            options.forEachIndexed { index, option ->
                                appendln("${'\u0030' + index}\u20E3 **${option.escapeMarkdown()}**")
                            }
                        }
                    }
                }
                timestamp {
                    millis(endsAt)
                }
                color = Color.RED
            }.build()).queue()
            channel.sendMessage(embed("Poll Results") {
                color = Color.CYAN
                description { +"Voting has ended! Here are the results" }

                var topVotes = 0
                val winners = mutableListOf<Int>()
                fields {
                    if (question != null) {
                        field {
                            title = "Question"
                            description = question ?: ""
                        }
                    }
                    field {
                        title = "Results"
                        description {
                            message.reactions.forEach { reaction ->
                                val value = reaction.reactionEmote.name[0] - '\u0030'
                                if (value !in 0 until options.size) return@forEach
                                options[value].let {
                                    appendln(
                                            "${reaction.reactionEmote.name} **$it** - __${reaction.count - 1} Votes__")
                                    if (reaction.count - 1 > topVotes) {
                                        winners.clear()
                                        topVotes = reaction.count - 1
                                        winners += value
                                    } else if (reaction.count - 1 == topVotes) {
                                        winners += value
                                    }
                                }
                            }
                        }
                    }

                    field {
                        title = "Winner"
                        description = winners.joinToString(prefix = "**",
                                postfix = "**") { options[it].escapeMarkdown() }
                    }
                }

                footer {
                    url = avatarUrl
                    text { +"Polled by $nameAndDiscrim" }
                }
            }.build()).queue()
        }
    }
}