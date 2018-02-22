package me.mrkirby153.KirBot.command.args

import java.util.Arrays

data class ResolverParameters(val params: Array<String>) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ResolverParameters

        if (!Arrays.equals(params, other.params)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(params)
    }
}