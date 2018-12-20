package me.mrkirby153.KirBot.command.executors.`fun`

import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.guild.starboard.StarboardEntry
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.StarboardModule
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.nameAndDiscrim

@Command(name = "star", clearance = 50)
class CommandStarboard : BaseCommand() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        throw CommandException("Invalid sub-command")
    }

    @Command(name = "update", clearance = 50, arguments = ["<mid:snowflake>"])
    fun update(context: Context, cmdContext: CommandContext) {
        context.channel.sendMessage(
                "Updating starboard for ${cmdContext.getNotNull<String>("mid")}").queue()
        ModuleManager[StarboardModule::class.java].updateStarboardMessage(context.guild,
                cmdContext.getNotNull("mid"))
    }

    @Command(name = "hide", clearance = 50, arguments = ["<mid:snowflake>"])
    fun hide(context: Context, cmdContext: CommandContext) {
        val entry = Model.where(StarboardEntry::class.java, "id",
                cmdContext.getNotNull("mid")).first()
                ?: throw CommandException("Message not found on the starboard")
        entry.hidden = true
        entry.save()
        ModuleManager[StarboardModule::class.java].updateStarboardMessage(context.guild, entry.id)
        context.send().success("Hidden ${cmdContext.getNotNull<String>("mid")} from the starboard",
                hand = true).queue()
    }

    @Command(name = "unhide", clearance = 50, arguments = ["<mid:snowflake>"])
    fun unhide(context: Context, cmdContext: CommandContext) {
        val entry = Model.where(StarboardEntry::class.java, "id",
                cmdContext.getNotNull("mid")).first()
                ?: throw CommandException("Message not found on the starboard")
        entry.hidden = false
        entry.save()
        ModuleManager[StarboardModule::class.java].updateStarboardMessage(context.guild, entry.id)
        context.send().success("Unhidden ${cmdContext.getNotNull<String>("mid")} from the starboard",
                hand = true).queue()
    }

    @Command(name = "block", clearance = 50, arguments = ["<user:snowflake>"])
    fun blockUser(context: Context, cmdContext: CommandContext) {
        val user = Bot.shardManager.getUser(cmdContext.getNotNull("user")) ?: throw CommandException("User not found")
        ModuleManager[StarboardModule::class.java].block(context.guild, user)
        context.send().success("Blocked ${user.nameAndDiscrim} from the starboard", true).queue()
    }

    @Command(name = "unblock", clearance = 50, arguments = ["<user:snowflake>"])
    fun unblockUser(context: Context, cmdContext: CommandContext) {
        val user = Bot.shardManager.getUser(cmdContext.getNotNull("user")) ?: throw CommandException("User not found")
        ModuleManager[StarboardModule::class.java].unblock(context.guild, user)
        context.send().success("Unblocked ${user.nameAndDiscrim} from the starboard", true).queue()
    }
}