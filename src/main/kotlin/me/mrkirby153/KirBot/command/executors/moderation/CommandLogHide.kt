package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.entities.User


class CommandModlogs {

    @Command(name = "hide", arguments = ["<user:user>"], clearance = CLEARANCE_ADMIN,
            category = CommandCategory.MODERATION, parent = "modlog")
    @LogInModlogs
    @CommandDescription("Hides a user from the modlogs")
    @IgnoreWhitelist
    fun hide(context: Context, cmdContext: CommandContext) {
        val userId = cmdContext.get<User>("user")?.id ?: return

        val currentlyHidden = context.kirbotGuild.extraData.optJSONArray(
                "log-ignored")?.map { it.toString() }?.toMutableSet() ?: mutableSetOf()

        currentlyHidden.add(userId)

        context.kirbotGuild.extraData.put("log-ignored", currentlyHidden)
        context.kirbotGuild.saveData()

        context.send().info(":ok_hand: Hidden `$userId` from the logs").queue()
    }

    @Command(name = "unhide", arguments = ["<user:user>"], clearance = CLEARANCE_ADMIN,
            category = CommandCategory.MODERATION, parent = "modlog")
    @LogInModlogs
    @CommandDescription("Unhides a user from the modlogs")
    @IgnoreWhitelist
    fun unhide(context: Context, cmdContext: CommandContext) {
        val userId = cmdContext.get<User>("user")?.id ?: return

        val currentlyHidden = context.kirbotGuild.extraData.optJSONArray(
                "log-ignored")?.map { it.toString() }?.toMutableSet() ?: mutableSetOf()

        currentlyHidden.remove(userId)

        context.kirbotGuild.extraData.put("log-ignored", currentlyHidden)
        context.kirbotGuild.saveData()

        context.send().info(":ok_hand: Unhidden `$userId` from the logs").queue()
    }

    @Command(name = "hidden", clearance = CLEARANCE_ADMIN, parent = "modlog",
            category = CommandCategory.MODERATION)
    @LogInModlogs
    @CommandDescription("List all the hidden users")
    @IgnoreWhitelist
    fun hidden(context: Context, cmdContext: CommandContext) {
        val currentlyHidden = context.kirbotGuild.extraData.optJSONArray(
                "log-ignored")?.map { it.toString() }?.toMutableSet() ?: mutableSetOf()
        context.send().text(buildString {
            appendln("The following users are hidden from the logs: ```")
            currentlyHidden.forEach {
                val user = Bot.shardManager.getUserById(it)
                if (user != null) {
                    appendln(user.nameAndDiscrim)
                } else {
                    appendln(it)
                }
            }
            append("```")
        }).queue()
    }

    @Command(name = "hush", clearance = CLEARANCE_ADMIN, category = CommandCategory.MODERATION,
            parent = "modlog")
    @LogInModlogs
    @CommandDescription("Hush the modlogs (Message deletes won't be logged)")
    @IgnoreWhitelist
    fun hush(context: Context, cmdContext: CommandContext) {
        context.kirbotGuild.logManager.hushed = true
        context.send().success(
                "Modlogs hushed :zipper_mouth: (Message deletes will not be logged)").queue()
    }

    @Command(name = "unhush", clearance = CLEARANCE_ADMIN, category = CommandCategory.MODERATION,
            parent = "modlog")
    @LogInModlogs
    @IgnoreWhitelist
    @CommandDescription("Unhush the modlogs")
    fun unhush(context: Context, cmdContext: CommandContext) {
        context.kirbotGuild.logManager.hushed = false
        context.send().success("Modlogs unhushed :open_mouth:").queue()
    }
}