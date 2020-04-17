package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.KirBot.utils.toTypedArray
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.sharding.ShardManager
import org.json.JSONArray
import javax.inject.Inject


class CommandModlogs @Inject constructor(private val shardManager: ShardManager){

    @Command(name = "hide", arguments = ["<user:user>"], clearance = CLEARANCE_ADMIN,
            category = CommandCategory.MODERATION, parent = "modlog")
    @LogInModlogs
    @CommandDescription("Hides a user from the modlogs")
    @IgnoreWhitelist
    fun hide(context: Context, cmdContext: CommandContext) {
        val userId = cmdContext.get<User>("user")?.id ?: return

        context.kirbotGuild.runWithExtraData(true) {
            val currentlyHidden = it.optJSONArray("log-ignored")
            if (currentlyHidden == null) {
                it.put("log-ignored", JSONArray(arrayOf(userId)))
            } else {
                if(currentlyHidden.contains(userId))
                    throw CommandException("That user is already hidden from the logs")
                currentlyHidden.put(userId)
            }
        }

        context.send().info(":ok_hand: Hidden `$userId` from the logs").queue()
    }

    @Command(name = "unhide", arguments = ["<user:user>"], clearance = CLEARANCE_ADMIN,
            category = CommandCategory.MODERATION, parent = "modlog")
    @LogInModlogs
    @CommandDescription("Unhides a user from the modlogs")
    @IgnoreWhitelist
    fun unhide(context: Context, cmdContext: CommandContext) {
        val userId = cmdContext.get<User>("user")?.id ?: return

        context.kirbotGuild.runWithExtraData(true) {
            val currentlyHidden = it.optJSONArray("log-ignored") ?: return@runWithExtraData
            val iter = currentlyHidden.iterator()
            var removed = false
            while(iter.hasNext()) {
                if(iter.next() == userId) {
                    removed = true
                    iter.remove()
                }
            }
            if(!removed)
                throw CommandException("That user was not hidden from the logs")
        }

        context.send().info(":ok_hand: Unhidden `$userId` from the logs").queue()
    }

    @Command(name = "hidden", clearance = CLEARANCE_ADMIN, parent = "modlog",
            category = CommandCategory.MODERATION)
    @LogInModlogs
    @CommandDescription("List all the hidden users")
    @IgnoreWhitelist
    fun hidden(context: Context, cmdContext: CommandContext) {
        var currentlyHidden: List<String> = emptyList()
        context.kirbotGuild.runWithExtraData {
            currentlyHidden = it.optJSONArray("log-ignored")?.toTypedArray(String::class.java) ?: emptyList()
        }
        if(currentlyHidden.isEmpty()) {
            context.channel.sendMessage("No users are hidden from the logs").queue()
            return
        }
        context.send().text(buildString {
            appendln("The following users are hidden from the logs: ```")
            currentlyHidden.forEach {
                val user = shardManager.getUserById(it)
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