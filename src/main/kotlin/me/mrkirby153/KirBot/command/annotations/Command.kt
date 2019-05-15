package me.mrkirby153.KirBot.command.annotations

import me.mrkirby153.KirBot.user.CLEARANCE_DEFAULT
import net.dv8tion.jda.core.Permission

@Retention(AnnotationRetention.RUNTIME)
//@Target(AnnotationTarget.FIELD)
annotation class Command(val name: String, val arguments: Array<String> = [],
                         val clearance: Int = CLEARANCE_DEFAULT,
                         val permissions: Array<Permission> = [], val admin: Boolean = false, val parent: String = "")