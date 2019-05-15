package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import me.mrkirby153.KirBot.utils.promptForConfirmation
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Game
import java.util.concurrent.TimeUnit

@Command(name = "clear-archives", admin = true)
class CommandClearArchives : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        promptForConfirmation(context,
                ":warning: Are you sure you want to delete **all** archives? This cannot be undone.",
                {
                    doClean(context)
                    true
                }, {
            context.channel.sendMessage("Canceled!").queue {
                it.deleteAfter(10, TimeUnit.SECONDS)
                context.deleteAfter(10, TimeUnit.SECONDS)
            }
            true
        })
    }

    private fun doClean(context: Context) {
        ModuleManager[Redis::class.java].getConnection().use { con ->
            val keys = con.keys("archive:*")
            if (keys.isEmpty()) {
                context.send().info(":warning: There are no archives to delete!").queue()
                return
            }
            val deleted = con.del(*(keys.toTypedArray()))
            context.send().success("Deleted `$deleted` archives", hand = true).queue()
        }
    }
}

@Command(name = "setstatus", admin = true,
        arguments = ["<status:string>", "<type:string>", "<game:string...>"])
class CommandSetStatus : BaseCommand() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val onlineStatus = try {
            OnlineStatus.valueOf(cmdContext.get<String>("status")!!.toUpperCase())
        } catch (e: IllegalArgumentException) {
            throw CommandException("The online status `${cmdContext.get<String>(
                    "status")}` was not found. Valid values are `${OnlineStatus.values().joinToString(
                    ", ")}`")
        }
        val gameType = try {
            Game.GameType.valueOf(cmdContext.get<String>("type")!!.toUpperCase())
        } catch (e: IllegalArgumentException) {
            throw CommandException("The game type `${cmdContext.get<String>(
                    "type")}` was not found. Valid values are `${Game.GameType.values().joinToString(
                    ", ")}`")
        }
        Bot.shardManager.autoUpdatePresence = false
        Bot.shardManager.onlineStatus = onlineStatus
        Bot.shardManager.playing = cmdContext.get<String>("game")!!
        Bot.shardManager.gameType = gameType
        Bot.shardManager.autoUpdatePresence = true
        Bot.shardManager.updatePresence()
        context.send().success("Presence has been updated!").queue()
    }

}