package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.CommandManager
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.core.MessageBuilder

class CommandSu : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = Bot.getUser(cmdContext.string("user") ?: "") ?: throw CommandException("User not found!")
        val command = cmdContext.string("command")

        val cntx = Context(user, user, context.channel, context.guild, context.shard, user.getMember(context.guild), MessageBuilder().append(command).build())
        Bot.LOG.warn("Executing command \"$command\" as ${cntx.author} - Requested by ${context.author}")
        CommandManager.execute(cntx, context.shard, context.guild)
    }
}