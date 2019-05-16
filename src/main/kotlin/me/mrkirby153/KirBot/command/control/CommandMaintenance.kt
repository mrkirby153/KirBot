package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import me.mrkirby153.KirBot.utils.promptForConfirmation
import me.mrkirby153.kcutils.utils.argparser.ArgumentParser
import me.mrkirby153.kcutils.utils.argparser.MissingFlagException
import me.mrkirby153.kcutils.utils.argparser.Option
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Game
import java.util.concurrent.TimeUnit


class CommandClearArchives {

    @Command(name = "clear-archives")
    @AdminCommand
    fun execute(context: Context, cmdContext: CommandContext) {
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


class CommandSetStatus {
    @Command(name = "setstatus",
            arguments = ["<status:string>", "[type:string]", "[game:string...]"])
    @AdminCommand
    fun execute(context: Context, cmdContext: CommandContext) {
        val onlineStatus = try {
            OnlineStatus.valueOf(cmdContext.getNotNull<String>("status").toUpperCase())
        } catch (e: IllegalArgumentException) {
            throw CommandException("The online status `${cmdContext.get<String>(
                    "status")}` was not found. Valid values are `${OnlineStatus.values().joinToString(
                    ", ")}`")
        }
        val gameTypeRaw = cmdContext.get<String>("type")
        val gameString = cmdContext.get<String>("game")
        if (gameTypeRaw != null) {
            val gameType = try {
                Game.GameType.valueOf(gameTypeRaw.toUpperCase())
            } catch (e: IllegalArgumentException) {
                throw CommandException("The game type `${cmdContext.get<String>(
                        "type")}` was not found. Valid values are `${Game.GameType.values().joinToString(
                        ", ")}`")
            }
            Bot.shardManager.setGame(Game.of(gameType, gameString))
        }
        if (gameTypeRaw == null && gameString == null) {
            Bot.shardManager.setGame(null)
        }
        Bot.shardManager.setStatus(onlineStatus)
        context.send().success("Presence has been updated!").queue()
    }

}

class CommandRestart {

    @Command(name = "restart", arguments = ["[query:string...]"])
    @AdminCommand
    fun restart(context: Context, cmdContext: CommandContext) {
        val parser = ArgumentParser()
        parser.addOption(Option("guild", required = false, aliases = arrayOf("g"),
                help = "The guild to restart"))
        parser.addOption(Option("shard", required = false, aliases = arrayOf("s"),
                help = "The shard to restart"))

        val parsed = try {
            parser.parse(cmdContext.get<String>("query") ?: "")
        } catch (e: java.lang.IllegalArgumentException) {
            throw CommandException(e.message)
        } catch (e: MissingFlagException) {
            throw CommandException(e.message)
        }
        val guild = parsed["guild"]
        val shard = parsed["shard"]

        if (guild != null) {
            val guildShard = guild.toLong().shr(22) % Bot.shardManager.shardsTotal
            context.channel.sendMessage(
                    "Guild $guild is on shard `$guildShard`. Restarting it...").queue()
            Bot.shardManager.restart(guildShard.toInt())
        }
        if (shard != null) {
            if (shard.equals("all", true)) {
                context.channel.sendMessage("Restarting all shards").queue()
                Bot.shardManager.restart()
            } else {
                context.channel.sendMessage("Restarting shard `$shard`").queue()
                Bot.shardManager.restart(shard.toInt())
            }
        }
    }
}