package com.mrkirby153.kirbot.services.command

import net.dv8tion.jda.api.Permission
import org.springframework.stereotype.Component

/**
 * Marker annotation designating the annotated method as a command
 */
@Target(AnnotationTarget.FUNCTION)
annotation class Command(
        /**
         * The name of the command
         */
        val name: String,

        /**
         * Any aliases of this command
         */
        val aliases: Array<String> = [],

        /**
         * The clearance required to execute this command
         */
        val clearance: Long,

        /**
         * True if this command should only be allowed to execute in whitelisted channels
         */
        val whitelist: Boolean = false,

        /**
         * True if this command will not show up in the help command
         */
        val hidden: Boolean = false,

        /**
         * True if this command should only be executable by bot admins
         */
        val admin: Boolean = false,

        /**
         * If non-empty, a list of permissions that the user must have to execute this command
         * if they do not have the required clearance
         */
        val userPermissions: Array<Permission> = [],

        /**
         * If non-empty, a list of permissions that the bot must have to execute this command
         */
        val permissions: Array<Permission> = [],

        /**
         * The category that this command resides in
         */
        val category: String = "",

        /**
         * True if this command should be logged in the server's modlogs
         */
        val audit: Boolean = true
)

/**
 * Annotation providing the command's description
 */
@Target(AnnotationTarget.FIELD)
annotation class Description(val value: String)

/**
 * Marker annotation for automatically discovering command methods
 */
@Target(AnnotationTarget.CLASS)
@Component
annotation class Commands