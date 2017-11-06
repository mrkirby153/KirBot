package me.mrkirby153.KirBot.command.executors.clearance

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.CommandManager
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.database.api.ClearanceOverride
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getClearance

class CommandOverrideClearance : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val cmd = cmdContext.string("command") ?: throw CommandException("Specify a command")
        val clearance = cmdContext.string("clearance") ?: throw CommandException("Specify clearance")

        val reset = clearance.equals("reset", true)

        val existingClearance = context.shard.clearanceOverrides[context.guild.id].firstOrNull {
            it.command.equals(cmd, true)
        }

        val cmdSpec = CommandManager.findCommand(cmd) ?: throw CommandException("That command isn't registered")

        if(context.user.getClearance(context.guild).value < cmdSpec.clearance.value){
            throw CommandException("You do not have permission to modify this command")
        }

        val targetClearance = Clearance.valueOf(clearance.toUpperCase())
        if(!reset && targetClearance.value > context.user.getClearance(context.guild).value){
            throw CommandException("You cannot change clearance to one higher than yours")
        }
        if (existingClearance != null) {
            if (reset) {
                existingClearance.delete().queue {
                    context.shard.clearanceOverrides[context.guild.id].removeIf {
                        it.command.equals(existingClearance.command, true)
                    }
                    context.send().success("Clearance reset!").queue()
                }
                return
            } else {
                try {
                    existingClearance.clearance = targetClearance
                } catch (e: Exception) {
                    context.send().error("Invalid clearance level").queue()
                }
                existingClearance.update().queue {
                    context.send().success("Set clearance to `$clearance`").queue()
                }
            }
        } else {
            try {
                val c = targetClearance
                ClearanceOverride.create(context.guild, cmd.toLowerCase(), c).queue {
                    context.shard.clearanceOverrides[context.guild.id].add(it)
                    context.send().success("Set clearance to `$clearance`").queue()
                }
            } catch (e: Exception) {
                context.send().error("Invalid clearance level").queue()
            }
        }
    }
}