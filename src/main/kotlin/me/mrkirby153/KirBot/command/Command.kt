package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.user.CLEARANCE_DEFAULT

@Retention(AnnotationRetention.RUNTIME)
annotation class Command(val name: String, val arguments: Array<String> = [],
                         val clearance: Int = CLEARANCE_DEFAULT, val control: Boolean = false)