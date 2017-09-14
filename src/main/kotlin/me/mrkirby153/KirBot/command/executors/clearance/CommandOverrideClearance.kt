package me.mrkirby153.KirBot.command.executors.clearance

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.database.api.ClearanceOverride
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context

class CommandOverrideClearance : CmdExecutor() {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val cmd = cmdContext.string("command") ?: throw CommandException("Specify a command")
        val clearance = cmdContext.string("clearance") ?: throw CommandException("Specify clearance")

        val reset = clearance.equals("reset", true)

        val existingClearance = context.shard.clearanceOverrides[context.guild.id].firstOrNull {
            it.command.equals(cmd, true)
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
                    existingClearance.clearance = Clearance.valueOf(clearance.toUpperCase())
                } catch (e: Exception) {
                    context.send().error("Invalid clearance level").queue()
                }
                existingClearance.update().queue {
                    context.send().success("Set clearance to `$clearance`").queue()
                }
            }
        } else {
            try {
                val c = Clearance.valueOf(clearance.toUpperCase())
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