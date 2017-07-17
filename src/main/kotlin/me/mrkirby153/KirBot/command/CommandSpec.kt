package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.command.args.CommandElement
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.user.Clearance
import net.dv8tion.jda.core.Permission

class CommandSpec(val command: String, spec: (CommandSpec.() -> Unit)? = null) {

    var aliases = mutableListOf<String>()

    var description: String = "No description provided"

    var clearance: Clearance = Clearance.USER

    val permissions = mutableListOf<Permission>()

    var category: CommandCategory = CommandCategory.UNCATEGORIZED

    val arguments = mutableListOf<CommandElement>()

    var ignoreWhitelist = false

    lateinit var executor: CmdExecutor

    init {
        if(spec != null){
            this.apply(spec)
        }
    }

    fun arguments(vararg elements: CommandElement) {
        this.arguments.addAll(elements)
    }

    fun permissions(vararg permissions: Permission){
        this.permissions.addAll(permissions)
    }
}