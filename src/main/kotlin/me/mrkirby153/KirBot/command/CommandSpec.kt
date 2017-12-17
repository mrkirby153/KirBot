package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.command.args.Argument
import me.mrkirby153.KirBot.user.Clearance

class CommandSpec(val respectWhitelist: Boolean = true, val category: CommandCategory = CommandCategory.UNCATEGORIZED, vararg args: Argument) {

    val arguments = mutableListOf<Argument>()
    val aliases = mutableListOf<String>()

    lateinit var executor: BaseCommand
    lateinit var clearance: Clearance

    init {
        arguments.addAll(args)
    }
}