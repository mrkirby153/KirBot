package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.listener.WaitUtils
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.AccessModule
import me.mrkirby153.KirBot.utils.Context
import net.dv8tion.jda.api.sharding.ShardManager
import javax.inject.Inject


class CommandGuildAccess @Inject constructor(private val accessModule: AccessModule, private val shardManager: ShardManager){

    @Command(name = "add", arguments = ["<list:string>", "<guild:snowflake>"],
            parent = "guild-access")
    @AdminCommand
    fun add(context: Context, cmdContext: CommandContext) {
        val guild = cmdContext.getNotNull<String>("guild")
        val list = try {
            AccessModule.WhitelistMode.valueOf(cmdContext.getNotNull<String>("list").toUpperCase())
        } catch (e: IllegalArgumentException) {
            throw CommandException(
                    "Not a valid access list. Valid lists are ${AccessModule.WhitelistMode.values().joinToString(
                            ", ")}")
        }

        accessModule.addToList(guild, list)
        context.send().success("Added `$guild` to the $list list").queue()
        if (list == AccessModule.WhitelistMode.BLACKLIST) {
            shardManager.getGuildById(guild)?.leave()?.queue()
        }
    }

    @Command(name = "remove", arguments = ["<list:string>", "<guild:snowflake>"],
            parent = "guild-access")
    @AdminCommand
    fun remove(context: Context, cmdContext: CommandContext) {
        val guild = cmdContext.getNotNull<String>("guild")

        val list = try {
            AccessModule.WhitelistMode.valueOf(cmdContext.getNotNull<String>("list").toUpperCase())
        } catch (e: IllegalArgumentException) {
            throw CommandException(
                    "Not a valid access list. Valid lists are ${AccessModule.WhitelistMode.values().joinToString(
                            ", ")}")
        }

        accessModule.removeFromList(guild, list)
        context.send().success("Removed `$guild` from the $list list").queue()
    }

    @Command(name = "list", arguments = ["<list:string>"], parent = "guild-access")
    @AdminCommand
    fun list(context: Context, cmdContext: CommandContext) {
        val list = try {
            AccessModule.WhitelistMode.valueOf(cmdContext.getNotNull<String>("list").toUpperCase())
        } catch (e: IllegalArgumentException) {
            throw CommandException(
                    "Not a valid access list. Valid lists are ${AccessModule.WhitelistMode.values().joinToString(
                            ", ")}")
        }

        val guilds = accessModule.getList(list).map { it ->
            if (shardManager.getGuildById(it) != null) {
                val guild = shardManager.getGuildById(it)!!
                "${guild.name} (`${guild.id}`)"
            } else {
                it
            }
        }

        var msg = "List: $list\n"
        guilds.forEach {
            val toAdd = " - $it\n"
            if (msg.length + toAdd.length >= 1990) {
                context.channel.sendMessage("$msg").queue()
                msg = ""
            } else {
                msg += toAdd
            }
        }
        if (msg != "")
            context.channel.sendMessage(msg).queue()
    }


    @Command(name = "importWhitelist", parent = "guild-access")
    @AdminCommand
    fun import(context: Context, cmdContext: CommandContext) {
        context.channel.sendMessage(
                "Are you sure you want to import all guilds into the whitelist?").queue { msg ->
            WaitUtils.confirmYesNo(msg, context.author, {
                shardManager.guilds.forEach { g ->
                    accessModule.addToList(g.id, AccessModule.WhitelistMode.WHITELIST)
                }
                msg.editMessage("Done! Added ${shardManager.guilds.size} to the list").queue()
            })
        }
    }
}