package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandExecutor
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.core.entities.User

@Command(name = "su", arguments = ["<user:user>", "<command:string,rest>"])
class CommandSu : BaseCommand(CommandCategory.ADMIN) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user")
        val command = cmdContext.get<String>("command")

        val c = Context(context.shard, context)
        c.customAuthor = user
        c.customMessage = command
        Bot.LOG.warn(
                "Executing command \"$command\" as ${c.author} - Requested by ${context.author}")
        CommandExecutor.execute(c)
    }
}