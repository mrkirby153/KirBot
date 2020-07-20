package com.mrkirby153.kirbot.utils

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.RestAction
import java.util.function.Consumer

/**
 * Gets a [Member] for the given user on the guild
 *
 * @param guild The guild to get the member on
 * @return The member, or null if the user is not a member of the provided guild
 */
fun User.getMember(guild: Guild): Member? = guild.getMember(this)

/**
 * Checks if the bot has the given permissions on a channel
 *
 * @param permission The permissions to check
 *
 * @return True if the bot has all permissions, false if otherwise
 */
fun <T : GuildChannel> T.checkPermissions(
        vararg permission: Permission) = this.guild.selfMember.hasPermission(this, *permission)

/**
 * Checks if the bot has the given permissions on a channel
 *
 * @param permission The permissions to check
 *
 * @return True if the bot has all permissions, false if otherwise
 */
fun MessageChannel.checkPermissions(
        vararg permission: Permission) = (this as? TextChannel)?.checkPermissions<TextChannel>(
        *permission) != false

/**
 * Checks if the bot has the given permissions on a channel
 *
 * @param permission The permissions to check
 *
 * @return True if the bot has all permissions, false if otherwise
 */
fun Guild.checkPermission(vararg permission: Permission) = this.selfMember.hasPermission(
        *permission)

/**
 * Queues a nullable rest action if it exists
 */
fun <T> RestAction<T>?.queue() {
    this?.queue()
}

/**
 * Queues a nullable rest action  if it exists
 */
fun <T> RestAction<T>?.queue(consumer: Consumer<T>) {
    this?.queue(consumer)
}