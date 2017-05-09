package me.mrkirby153.KirBot.command.executors.server


import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.success
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

@Command(name = "clean", clearance = Clearance.BOT_MANAGER, description = "Delete the last messages in the channel",
        requiredPermissions = arrayOf(Permission.MESSAGE_MANAGE, Permission.MESSAGE_WRITE), category = "Moderation")
class CommandClean : CommandExecutor() {

    override fun execute(message: Message, args: Array<String>) {
        if (args.isEmpty()) {
            message.send().error("Please provide the number of messages to delete. (Max 100)")
            return
        }
        try {
            val messageCount = Integer.parseInt(args[0])
            if (messageCount > 100) {
                message.send().error("I can only delete 100 messages at a time")
            }
            if (message.channel is TextChannel) {
                val channel = message.channel as TextChannel
                channel.history.retrievePast(messageCount).queue { m ->
                    val m1 = m.filter { it.creationTime.isAfter(OffsetDateTime.now().minusDays(14)) }
                    channel.deleteMessages(m1).queue {
                        message.send().success("Deleted $messageCount messages!").queue {
                            m ->
                            m.delete().queueAfter(10, TimeUnit.SECONDS)
                        }
                    }
                }
            }
        } catch (e: NumberFormatException) {
            message.send().error(args[0] + " is not a number!")
        }

    }
}