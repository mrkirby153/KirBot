package me.mrkirby153.KirBot.command.executors.polls

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.embed.b
import me.mrkirby153.KirBot.utils.embed.embed
import me.mrkirby153.KirBot.utils.localizeTime
import me.mrkirby153.KirBot.utils.mdEscape
import java.awt.Color
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class CommandPoll : CmdExecutor() {

    val time = mutableMapOf<String, Int>()

    init {
        time.clear()
        time.put("s", 1)
        time.put("m", 60)
        time.put("h", 3600)
        time.put("d", 86400)
        time.put("w", 604800)
    }

    override fun execute(context: Context, cmdContext: CommandContext) {
        // !poll 10m :dog:
        val duration = timeOffset(cmdContext.string("duration") ?: "0s")

        if (duration <= 0) {
            throw CommandException("Please specify a duration greater than zero!")
        }

        if (duration > timeOffset("1w")) {
            throw CommandException("Polls can only be less than one week!")
        }

        val data = cmdContext.get<String>("options") ?: ""
        val split = data.split(";")
        val question: String? = if (split.size == 2) split[0] else null
        val rawOptions = if (split.size == 2) split[1] else data
        if (rawOptions.isEmpty()) {
            throw CommandException("Please specify options for the poll")
        }

        val options = rawOptions.split(",").map(String::trim).filter { it.isNotBlank() }.map(String::mdEscape)

        if (options.size <= 1) {
            throw CommandException("Please provide more than one option for the poll")
        }

        if (options.size > 9) {
            throw CommandException("You can only have 9 options for the poll!")
        }
        context.send().embed("Poll") {
            color = Color.GREEN
            description { +"Vote by clicking the reactions on the choices below! Results will be final in ${b(localizeTime(duration))}" }
            fields {
                if(question != null)
                    field {
                        title = "Question"
                        description = question
                    }
                field {
                    title = "Options"
                    description {
                        options.filter { it.isNotEmpty() }.forEachIndexed {index, option ->
                            appendln("${'\u0030' + (index)}\u20E3 **$option**")
                        }
                    }
                }
            }
        }.rest().queue {
            val m = it
            for (index in 0..options.size - 1) {
                it.addReaction("${'\u0030' + (index)}\u20E3").queue()
            }

            it.editMessage(embed("Poll") {
                description {
                    +"Voting has ended, Check newer messages for results"
                }
                color = Color.RED
            }.build()).queueAfter(duration.toLong(), TimeUnit.SECONDS) {
                m.unpin().queue()
                context.send().embed("Poll Results") {
                    color = Color.CYAN
                    description {  +"Voting has ended! Here are the results" }

                    var topVotes = 0
                    val winners = mutableListOf<Int>()
                    fields {
                        if(question != null)
                        field{
                            title = "Question"
                            description  = question
                        }

                        field {
                            title = "results"
                            description {
                                it.reactions.forEach { reaction ->
                                    val value = reaction.emote.name[0] - '\u0030'
                                    if (value !in 0 until options.size) return@forEach

                                    options[value].let {
                                        appendln("${reaction.emote.name} **$it** — __${reaction.count - 1} Votes__")

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
                            description  = winners.joinToString(prefix = "**", postfix = "**") { options[it] }
                        }
                    }
                }.rest().queue()
            }
        }
    }


    private fun timeOffset(timestamp: String): Int {
        // Define regex to match one or more digits and a letter
        val dateRegex = "(\\d+\\D)"
        // Compile it
        val datePattern = Pattern.compile(dateRegex)
        // Create a matcher
        val dateMatcher = datePattern.matcher(timestamp)
        // Compile regex to match the letter part of the timestamp
        val multiplierRegex = Pattern.compile("(\\D+)")
        // Compile regex to match the number part of the timestamp
        val timeRegex = Pattern.compile("(\\d+)")
        // Define a time offset variable
        var timeOffset = 0
        // For every "\d\D" in the timestamp
        while (dateMatcher.find()) {
            // Get the time plus the multiplier
            val part = timestamp.substring(dateMatcher.start(), dateMatcher.end())
            // Compile matchers
            val multiplierMatcher = multiplierRegex.matcher(part)
            val timeMatcher = timeRegex.matcher(part)
            var mult = "0"
            var time = "0"
            // Find the first occurance of a multiplier
            if (multiplierMatcher.find()) {
                mult = part.substring(multiplierMatcher.start(), multiplierMatcher.end())
            }
            // Find the first occurance of the time
            if (timeMatcher.find()) {
                time = part.substring(timeMatcher.start(), timeMatcher.end())
            }
            // Add the time times the multiplier to the timeOffset variable
            timeOffset += (Integer.parseInt(time) * timeMult(mult))
        }
        return timeOffset
    }

    private fun timeMult(`val`: String): Int {
        return this.time[`val`] ?: return 0
    }


}