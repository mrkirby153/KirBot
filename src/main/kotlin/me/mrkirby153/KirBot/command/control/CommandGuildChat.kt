package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import net.dv8tion.jda.core.MessageBuilder
import java.util.concurrent.TimeUnit

@Command(name = "gchat", admin = true, arguments = ["<channel:string>", "[msg:string...]"])
class CommandGuildChat : BaseCommand() {

    private val lastChannel = mutableMapOf<String, String>()

    override fun execute(context: Context, cmdContext: CommandContext) {
        val channelRaw = cmdContext.get<String>("channel")!!
        val channel = if (channelRaw == "-") lastChannel[context.author.id] ?: throw CommandException(
                "You attempted to use the last channel but you have not sent messages") else channelRaw

        lastChannel[context.author.id] = channel

        val resolvedChannel = Bot.shardManager.shards.flatMap { it.guilds }.flatMap { it.textChannels }.firstOrNull { it.id == channel } ?: throw CommandException(
                "That channel was not found!")
        val msg = cmdContext.get<String>("msg")

        if (context.attachments.isNotEmpty()) {
            val attachment = context.attachments.first()
            val m = if(msg != null) MessageBuilder().append(msg).build() else null
            Bot.LOG.debug("File size is ${attachment.size}")
           val fileMsg = if(attachment.size > 102400) context.send().info("Sending a file, this may take some time...").complete() else null
            resolvedChannel.sendFile(attachment.inputStream, attachment.fileName, m).queue{
                context.success()
                context.deleteAfter(60, TimeUnit.SECONDS)
                fileMsg?.delete()?.queue()
            }
        } else {
            if(msg == null)
                throw CommandException("Specify a message to send")
            resolvedChannel.sendMessage(msg).queue{
                context.success()
                context.deleteAfter(60, TimeUnit.SECONDS)
            }
        }
    }
}