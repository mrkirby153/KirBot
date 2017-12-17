package me.mrkirby153.KirBot.command

import me.mrkirby153.KirBot.user.Clearance

@Retention(AnnotationRetention.RUNTIME)
annotation class RequiresClearance(val value: Clearance)