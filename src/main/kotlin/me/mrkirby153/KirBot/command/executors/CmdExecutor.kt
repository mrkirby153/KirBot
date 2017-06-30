package me.mrkirby153.KirBot.command.executors

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context

abstract class CmdExecutor {


    abstract fun execute(context: Context, cmdContext: CommandContext)
}