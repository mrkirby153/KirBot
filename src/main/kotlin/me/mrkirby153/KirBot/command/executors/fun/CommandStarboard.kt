package me.mrkirby153.KirBot.command.executors.`fun`

import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.guild.starboard.StarboardEntry
import me.mrkirby153.KirBot.modules.StarboardModule
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.api.sharding.ShardManager
import javax.inject.Inject

class CommandStarboard @Inject constructor(private val starboardModule: StarboardModule, private val shardManager: ShardManager) {

    @Command(name = "update", clearance = CLEARANCE_MOD, arguments = ["<mid:snowflake>"],
            parent = "starboard", category = CommandCategory.FUN)
    @LogInModlogs
    @CommandDescription("Forces an update of the starboard message")
    fun update(context: Context, cmdContext: CommandContext) {
        context.channel.sendMessage(
                "Updating starboard for ${cmdContext.getNotNull<String>("mid")}").queue()
        starboardModule.updateStarboardMessage(context.guild,
                cmdContext.getNotNull("mid"))
    }

    @Command(name = "hide", clearance = CLEARANCE_MOD, arguments = ["<mid:snowflake>"],
            parent = "starboard", category = CommandCategory.FUN)
    @LogInModlogs
    @CommandDescription("Hides an entry from the starboard")
    fun hide(context: Context, cmdContext: CommandContext) {
        val entry = Model.where(StarboardEntry::class.java, "id",
                cmdContext.getNotNull("mid")).first()
                ?: throw CommandException("Message not found on the starboard")
        entry.hidden = true
        entry.save()
        starboardModule.updateStarboardMessage(context.guild, entry.id)
        context.send().success("Hidden ${cmdContext.getNotNull<String>("mid")} from the starboard",
                hand = true).queue()
    }

    @Command(name = "unhide", clearance = CLEARANCE_MOD, arguments = ["<mid:snowflake>"],
            parent = "starboard", category = CommandCategory.FUN)
    @LogInModlogs
    @CommandDescription("Unhides an entry from the starboard")
    fun unhide(context: Context, cmdContext: CommandContext) {
        val entry = Model.where(StarboardEntry::class.java, "id",
                cmdContext.getNotNull("mid")).first()
                ?: throw CommandException("Message not found on the starboard")
        entry.hidden = false
        entry.save()
        starboardModule.updateStarboardMessage(context.guild, entry.id)
        context.send().success(
                "Unhidden ${cmdContext.getNotNull<String>("mid")} from the starboard",
                hand = true).queue()
    }

    @Command(name = "block", clearance = CLEARANCE_MOD, arguments = ["<user:snowflake>"],
            parent = "starboard", category = CommandCategory.FUN)
    @LogInModlogs
    @CommandDescription(
            "Blocks a user from the starboard. They cannot star messages and their messages cannot be starred")
    fun blockUser(context: Context, cmdContext: CommandContext) {
        val user = shardManager.getUserById(cmdContext.getNotNull<String>("user"))
                ?: throw CommandException("User not found")
        starboardModule.block(context.guild, user)
        context.send().success("Blocked ${user.nameAndDiscrim} from the starboard", true).queue()
    }

    @Command(name = "unblock", clearance = CLEARANCE_MOD, arguments = ["<user:snowflake>"],
            parent = "starboard", category = CommandCategory.FUN)
    @LogInModlogs
    @CommandDescription("Unblocks a user from the starboard")
    fun unblockUser(context: Context, cmdContext: CommandContext) {
        val user = shardManager.getUserById(cmdContext.getNotNull<String>("user"))
                ?: throw CommandException("User not found")
        starboardModule.unblock(context.guild, user)
        context.send().success("Unblocked ${user.nameAndDiscrim} from the starboard", true).queue()
    }
}