package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.entities.User
import java.util.concurrent.TimeUnit

@Command(name = "clearance", arguments = ["[user:user]"], clearance = Clearance.BOT)
class CommandClearance : BaseCommand(CommandCategory.ADMIN) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: context.author
        context.send().info("${user.nameAndDiscrim}'s clearance is `${user.getClearance(
                context.guild)}`").queue {
            it.deleteAfter(30, TimeUnit.SECONDS)
            context.deleteAfter(30, TimeUnit.SECONDS)
        }
    }
}