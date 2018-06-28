package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.user.CLEARANCE_DEFAULT
import net.dv8tion.jda.core.Permission

@Retention(AnnotationRetention.RUNTIME)
annotation class Command(val name: String, val arguments: Array<String> = [],
                         val clearance: Int = CLEARANCE_DEFAULT, val control: Boolean = false,
                         val permissions: Array<Permission> = [])