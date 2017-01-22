package me.mrkirby153.KirBot.command.executors.server


import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.executors.CommandExecutor
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Note
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import java.time.OffsetDateTime

@Command(name = "clean", clearance = Clearance.BOT_MANAGER, description = "Delete the last messages in the channel")
class CommandClean : CommandExecutor() {

    override fun execute(note: Note, server: Server, sender: User, channel: MessageChannel, args: Array<String>) {
        if (args.isEmpty()) {
            note.error("Please provide the number of messages to delete. (Max 100)")
            return
        }
        try {
            val messageCount = Integer.parseInt(args[0])
            if (messageCount > 100) {
                note.error("I can only delete 100 messages at a time")
            }
            if (channel is TextChannel) {
                channel.history.retrievePast(messageCount).queue { m ->
                  val m1 =  m.filter { it.creationTime.isAfter(OffsetDateTime.now().minusDays(14)) }
                    channel.deleteMessages(m1).queue { v ->
                        note.success("Deleted $messageCount messages!").get().delete(10)
                    }
                }
            }
        } catch (e: NumberFormatException) {
            note.error(args[0] + " is not a number!")
        }

    }
}