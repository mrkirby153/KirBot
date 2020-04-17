package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class CommandGuildChat @Inject constructor(private val shardManager: ShardManager) {

    private val lastChannel = mutableMapOf<String, String>()
    @AdminCommand
    @Command(name = "gchat", arguments = ["<channel:string>", "[msg:string...]"])
    fun execute(context: Context, cmdContext: CommandContext) {
        val channelRaw = cmdContext.get<String>("channel")!!
        val channel = if (channelRaw == "-") lastChannel[context.author.id]
                ?: throw CommandException(
                        "You attempted to use the last channel but you have not sent messages") else channelRaw

        lastChannel[context.author.id] = channel

        val resolvedChannel = shardManager.shards.flatMap { it.guilds }.flatMap { it.textChannels }.firstOrNull { it.id == channel }
                ?: throw CommandException(
                        "That channel was not found!")
        val msg = cmdContext.get<String>("msg")

        if (context.attachments.isNotEmpty()) {
            val attachment = context.attachments.first()
            val m = if (msg != null) MessageBuilder().append(msg).build() else null
            Bot.LOG.debug("File size is ${attachment.size}")
            val fileMsg = context.send().info("Sending a file, this may take some time...").complete()
            attachment.retrieveInputStream().thenApply { inputStream ->
                if(m != null) {
                    resolvedChannel.sendMessage(m).addFile(inputStream, attachment.fileName).queue {
                        context.success()
                        fileMsg.delete().queue()
                    }
                } else {
                    resolvedChannel.sendFile(inputStream, attachment.fileName).queue {
                        context.success()
                        fileMsg.delete().queue()
                    }
                }
            }
        } else {
            if (msg == null)
                throw CommandException("Specify a message to send")
            resolvedChannel.sendMessage(msg).queue {
                context.success()
                context.deleteAfter(60, TimeUnit.SECONDS)
            }
        }
    }
}