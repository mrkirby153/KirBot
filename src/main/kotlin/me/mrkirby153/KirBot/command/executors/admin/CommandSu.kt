package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandExecutor
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getMember
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import javax.inject.Inject

class CommandSu @Inject constructor(private val commandExecutor: CommandExecutor){

    @Command(name = "sudo", arguments = ["<user:user>", "<command:string...>"])
    @AdminCommand
    @CommandDescription("Runs commands as other users")
    fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user")
        val command = cmdContext.get<String>("command")

        val c = SudoContext(context, user, command)
        Bot.LOG.warn(
                "Executing command \"$command\" as ${c.author} - Requested by ${context.author}")
        commandExecutor.execute(c)
    }

    class SudoContext(message: Message, val customAuthor: User? = null,
                      val customMsg: String? = null) : Context(message) {

        override fun getAuthor(): User {
            return customAuthor ?: super.getAuthor()
        }

        override fun getContentRaw(): String {
            return customMsg ?: super.getContentRaw()
        }

        override fun getMember(): Member? {
            return customAuthor?.getMember(guild) ?: super.getMember()
        }
    }
}