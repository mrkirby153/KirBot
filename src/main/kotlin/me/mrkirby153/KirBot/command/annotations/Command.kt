package me.mrkirby153.KirBot.command.annotations

import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.user.CLEARANCE_DEFAULT
import net.dv8tion.jda.api.Permission

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Command(val name: String, val aliases: Array<String> = [], val arguments: Array<String> = [],
                         val clearance: Int = CLEARANCE_DEFAULT,
                         val permissions: Array<Permission> = [], val parent: String = "",
                         val category: CommandCategory = CommandCategory.UNCATEGORIZED)