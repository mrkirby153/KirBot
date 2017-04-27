package me.mrkirby153.KirBot.command.executors.polls

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import java.awt.Color
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

@Command(name = "poll", description = "Create polls!", clearance = Clearance.USER, requiredPermissions = arrayOf(Permission.MESSAGE_ADD_REACTION))
class CommandPoll : CommandExecutor() {

    val time = mutableMapOf<String, Int>()

    init {
        time.clear()
        time.put("s", 1)
        time.put("m", 60)
        time.put("h", 3600)
        time.put("d", 86400)
        time.put("w", 604800)
    }

    override fun execute(message: Message, args: Array<String>) {
        // !poll 10m :dog:
        val duration = timeOffset(args[0])
        val options = args.drop(1).joinToString(" ").split(",").map(String::trim)

        if (options.size <= 1) {
            message.send().error("Please provide more than one option for the poll!").queue()
            return
        }

        if(options.size > 9){
            message.send().error("You can only have 9 options in a poll!").queue()
            return
        }
        // TODO 4/25/2017 Fix the brokenness!
        message.send().embed("Poll") {
            description = "Vote by clicking the reactions on the choices below! Results will be final in ${localizeTime(duration)}"
            field("Options") {
                buildString {
                    options.forEachIndexed { index, option ->
                        appendln("${'\u0030' + (index+1)}\u20E3 **$option**")
                    }
                }
            }
        }.rest().queue {
            val m = it
            m.pin().queue()
            for(index in 0..options.size - 1){
                it.addReaction("${'\u0030' + (index+1)}\u20E3").queue()
            }

            it.editMessage(it.embeds[0].edit().apply {
                description = "**Voting ended**! Check new message for results"
            }.build()).queueAfter(duration.toLong(), TimeUnit.SECONDS) {
                m.unpin().queue()
                it.send().embed("Poll Results") {
                    color = Color.CYAN
                    description = "Voting has ended! Here are the results!"

                    var topVotes = 0
                    val winners = mutableListOf<Int>()

                    field("Results") {
                        buildString {
                            it.reactions.forEach { reaction ->
                                val value = reaction.emote.name[0] - '\u0030'
                                if (value !in 0..options.size - 1) return@forEach

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

                    field("Winner") {
                        winners.joinToString(prefix = "**", postfix = "**") { options[it] }
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

    private fun localizeTime(time: Int): String {
        if (time < 60) {
            return "$time seconds"
        } else if (time < 3600) {
            return "${time.toDouble() / 60} minutes"
        } else if (time < 86400) {
            return "${(time.toDouble() / 3600)} hours"
        } else if (time < 604800) {
            return "${time.toDouble() / 86400} days"
        } else {
            return "${time.toDouble() / 604800} weeks"
        }
    }
}