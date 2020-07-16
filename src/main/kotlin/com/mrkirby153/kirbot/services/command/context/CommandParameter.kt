package com.mrkirby153.kirbot.services.command.context

import java.lang.reflect.Parameter

/**
 * A parameter for a command
 */
class CommandParameter(
        /**
         * The type of the parameter
         */
        val type: Class<*>,
        /**
         * The java parameter for the class
         */
        val parameter: Parameter,
        /**
         * The name of the parameter
         */
        val name: String,
        /**
         * If the parameter can be nullable
         */
        val nullable: Boolean,
        /**
         * The total number of parameters
         */
        val parameterCount: Int,
        /**
         * The current index of the parameter
         */
        val parameterIndex: Int)