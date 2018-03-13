package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.user.Clearance

@Retention(AnnotationRetention.RUNTIME)
annotation class Command(val name: String, val arguments: Array<String> = [],
                         val clearance: Clearance = Clearance.USER, val control: Boolean = false)