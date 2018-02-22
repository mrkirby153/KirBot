package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.Context
import java.lang.reflect.Method

abstract class BaseCommand(val respectWhitelist: Boolean = true,
                           val category: CommandCategory = CommandCategory.UNCATEGORIZED) {

    constructor(category: CommandCategory) : this(true, category)

    var cmdPrefix = ""
    var aliasUsed = ""

    val aliases = mutableListOf<String>()
    val clearance: Clearance
    val argumentList: Array<String>

    val subCommands: Array<String>
        get() {
            val list = mutableListOf<String>()
            this.javaClass.declaredMethods.forEach { it ->
                val an = it.getDeclaredAnnotation(Command::class.java) ?: return@forEach
                list.add(an.name)
            }
            return list.toTypedArray()
        }

    init {
        val annotation = this.javaClass.getAnnotation(Command::class.java)
                ?: throw RuntimeException("${this.javaClass} is missing the @Command annotation")
        clearance = annotation.clearance
        argumentList = annotation.arguments
        aliases.addAll(annotation.name.split(",").map { it.toLowerCase() })
        Bot.LOG.debug("Constructing ${aliases.joinToString(
                ", ")} with clearance $clearance (${argumentList.joinToString(",")})")
    }

    abstract fun execute(context: Context, cmdContext: CommandContext)

    fun hasSubCommand(name: String): Boolean {
        return getSubCommand(name) != null
    }

    fun isAlias(command: String) = command.toLowerCase() in aliases.map { it.toLowerCase() }

    fun getSubCommand(name: String): Method? {
        Bot.LOG.debug("[SUBCMD] Looking up command $name")
        return this.javaClass.declaredMethods.firstOrNull {
            Bot.LOG.debug("[SUBCMD] Checking candidate ${it.name}")
            it.getDeclaredAnnotation(
                    Command::class.java) != null && name.toLowerCase() in it.getDeclaredAnnotation(
                    Command::class.java).name.toLowerCase().split(",")
        }
    }

    fun invokeSubCommand(name: String, context: Context, cmdContext: CommandContext) {
        Bot.LOG.debug("Invoking sub-command $name")
        getSubCommand(name)?.invoke(this, context, cmdContext)
    }
}