package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.entities.User

@Command(name = "log-hide", arguments = ["<user:user>"], clearance = CLEARANCE_ADMIN)
@LogInModlogs
class CommandLogHide : BaseCommand(false, CommandCategory.MODERATION) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val userId = cmdContext.get<User>("user")?.id ?: return

        val currentlyHidden = context.kirbotGuild.extraData.optJSONArray(
                "log-ignored")?.map { it.toString() }?.toMutableSet() ?: mutableSetOf()

        currentlyHidden.add(userId)

        context.kirbotGuild.extraData.put("log-ignored", currentlyHidden)
        context.kirbotGuild.saveData()

        context.send().info(":ok_hand: Hidden `$userId` from the logs").queue()
    }
}

@Command(name = "log-show", arguments = ["<user:user>"], clearance = CLEARANCE_ADMIN)
@LogInModlogs
class CommandLogUnhide : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val userId = cmdContext.get<User>("user")?.id ?: return

        val currentlyHidden = context.kirbotGuild.extraData.optJSONArray(
                "log-ignored")?.map { it.toString() }?.toMutableSet() ?: mutableSetOf()

        currentlyHidden.remove(userId)

        context.kirbotGuild.extraData.put("log-ignored", currentlyHidden)
        context.kirbotGuild.saveData()

        context.send().info(":ok_hand: Unhidden `$userId` from the logs").queue()
    }
}

@Command(name = "log-hidden", clearance = CLEARANCE_ADMIN)
class CommandLogHidden : BaseCommand(false, CommandCategory.MODERATION) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val currentlyHidden = context.kirbotGuild.extraData.optJSONArray(
                "log-ignored")?.map { it.toString() }?.toMutableSet() ?: mutableSetOf()
        context.send().text(buildString {
            appendln("The following users are hidden from the logs: ```")
            currentlyHidden.forEach {
                val user = Bot.shardManager.getUser(it)
                if (user != null) {
                    appendln(user.nameAndDiscrim)
                } else {
                    appendln(it)
                }
            }
            append("```")
        }).queue()
    }

}