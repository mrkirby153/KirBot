package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context

abstract class BaseCommand {

    var cmdPrefix = ""
    var aliasUsed = ""

    abstract fun execute(context: Context, cmdContext: CommandContext)

    open fun getSpec(): CommandSpec {
        return CommandSpec()
    }
}