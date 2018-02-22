package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.listener.WaitUtils
import me.mrkirby153.KirBot.scheduler.Schedulable
import me.mrkirby153.KirBot.scheduler.Scheduler
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.RED_CROSS
import me.mrkirby153.KirBot.utils.deleteAfter
import me.mrkirby153.kcutils.Time
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.guild.react.GuildMessageReactionAddEvent
import java.util.concurrent.TimeUnit

@Command(name = "remindMe,remind", arguments = ["<time:string>", "<query:string,rest>"])
class CommandRemindMe : BaseCommand(false, CommandCategory.FUN) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val time = Time.parse(cmdContext.get<String>("time")!!)
        val query = cmdContext.get<String>("query")!!

        if (query.isBlank()) {
            throw CommandException("I need something to remind you about!")
        }

        Scheduler.submit(RemindAction(context.guild.id, context.channel.id, context.author.id,
                query), time, TimeUnit.MILLISECONDS)
        context.send().success("Ok, I will remind you in `${Time.format(1, time,
                smallest = Time.TimeUnit.SECONDS)}` about `$query`", true).queue {
            it.deleteAfter(10, TimeUnit.SECONDS)
            context.deleteAfter(10, TimeUnit.SECONDS)
        }
    }

    class RemindAction(val guild: String, val channel: String, val user: String,
                       val query: String) : Schedulable {

        override fun run() {
            Bot.shardManager.getUser(user)?.let { user ->
                Bot.shardManager.getGuild(guild)?.let {
                    it.getTextChannelById(channel)?.sendMessage(
                            "<@$user> You asked me to remind you about `$query`. React with $RED_CROSS to delete this message")?.queue {
                        it.addReaction(RED_CROSS).queue()
                        queueDelete(it, user)
                    }
                }
            }
        }

        private fun queueDelete(it: Message,
                                user: User) {
            WaitUtils.waitFor(GuildMessageReactionAddEvent::class.java) { event ->
                it.channel.getMessageById(event.messageId).queue { msg ->
                    if (msg.id == it.id && event.reactionEmote.name == RED_CROSS && event.user == user) {
                        Bot.LOG.debug("Reaction request delete")
                        msg.delete().queue()
                        cancel()
                    }
                }
            }
        }

    }
}