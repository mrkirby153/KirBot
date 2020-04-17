package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.listener.WaitUtils
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import me.mrkirby153.KirBot.utils.promptForConfirmation
import me.mrkirby153.kcutils.utils.argparser.ArgumentParser
import me.mrkirby153.kcutils.utils.argparser.MissingArgumentException
import me.mrkirby153.kcutils.utils.argparser.Option
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.sharding.ShardManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class CommandClearArchives @Inject constructor(private val redis: Redis){

    @Command(name = "clear-archives", permissions = [Permission.MESSAGE_ADD_REACTION])
    @AdminCommand
    fun execute(context: Context, cmdContext: CommandContext) {
        context.channel.sendMessage(":warning: Are you sure you want to delete **all** archives? This cannot be undone").queue { msg ->
            WaitUtils.confirmYesNo(msg, context.author, {
                doClean(context)
            }, {
                msg.editMessage("Canceled!").queue {
                    it.deleteAfter(10, TimeUnit.SECONDS)
                    context.deleteAfter(10, TimeUnit.SECONDS)
                }
            })
        }
    }

    private fun doClean(context: Context) {
        redis.getConnection().use { con ->
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


class CommandSetStatus @Inject constructor(private val shardManager: ShardManager){
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
                Activity.ActivityType.valueOf(gameTypeRaw.toUpperCase())
            } catch (e: IllegalArgumentException) {
                throw CommandException("The game type `${cmdContext.get<String>(
                        "type")}` was not found. Valid values are `${Activity.ActivityType.values().joinToString(
                        ", ")}`")
            }
            shardManager.setActivity(Activity.of(gameType, gameString?: ""))
        }
        if (gameTypeRaw == null && gameString == null) {
            shardManager.setActivity(null)
        }
        shardManager.setStatus(onlineStatus)
        context.send().success("Presence has been updated!").queue()
    }

}

class CommandRestart @Inject constructor(private val shardManager: ShardManager){

    @Command(name = "restart", arguments = ["[query:string...]"])
    @AdminCommand
    fun restart(context: Context, cmdContext: CommandContext) {
        val parser = ArgumentParser()
        parser.addOption(Option("--guild", required = false, aliases = arrayOf("-g"),
                help = "The guild to restart"))
        parser.addOption(Option("--shard", required = false, aliases = arrayOf("-s"),
                help = "The shard to restart"))

        val parsed = try {
            parser.parse(cmdContext.get<String>("query") ?: "")
        } catch (e: java.lang.IllegalArgumentException) {
            throw CommandException(e.message)
        } catch (e: MissingArgumentException) {
            throw CommandException(e.message)
        }
        val guild = parsed["guild"]
        val shard = parsed["shard"]

        if (guild != null) {
            val guildShard = guild.toLong().shr(22) % shardManager.shardsTotal
            context.channel.sendMessage(
                    "Guild $guild is on shard `$guildShard`. Restarting it...").queue()
            shardManager.restart(guildShard.toInt())
        }
        if (shard != null) {
            if (shard.equals("all", true)) {
                context.channel.sendMessage("Restarting all shards").queue()
                shardManager.restart()
            } else {
                context.channel.sendMessage("Restarting shard `$shard`").queue()
                shardManager.restart(shard.toInt())
            }
        }
    }
}