package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_DEFAULT
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import me.mrkirby153.KirBot.utils.getClearance
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.api.entities.User
import java.util.concurrent.TimeUnit


class CommandClearance {

    @Command(name = "clearance", arguments = ["[user:user]"], clearance = CLEARANCE_DEFAULT, category = CommandCategory.MISCELLANEOUS)
    @CommandDescription("Displays the user's clearance")
    fun execute(context: Context, cmdContext: CommandContext) {
        val user = cmdContext.get<User>("user") ?: context.author
        context.send().info("${user.nameAndDiscrim}'s clearance level is `${user.getClearance(
                context.guild)}`").queue {
            it.deleteAfter(30, TimeUnit.SECONDS)
            context.deleteAfter(30, TimeUnit.SECONDS)
        }
    }
}