package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.command.args.Argument
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context

abstract class BaseCommand(respectWhitelist: Boolean = true, category: CommandCategory = CommandCategory.UNCATEGORIZED,
                           vararg arguments: Argument) {

    constructor(vararg arguments: Argument) : this(true, CommandCategory.UNCATEGORIZED, *arguments)
    constructor(category: CommandCategory, vararg arguments: Argument) : this(true, category, *arguments)

    var cmdPrefix = ""
    var aliasUsed = ""

    val commandSpec = CommandSpec(respectWhitelist, category, *arguments)

    abstract fun execute(context: Context, cmdContext: CommandContext)
}