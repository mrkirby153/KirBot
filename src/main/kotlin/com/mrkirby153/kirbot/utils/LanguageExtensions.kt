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
 * Checks if the bot has the given permissions on a channel. If this is a private message channel
 * this will return true
 *
 * @param permission The permissions to check
 *
 * @return True if the bot has all permissions, false if otherwise
 */
fun MessageChannel.checkPermissions(
        vararg permission: Permission) = if (this is TextChannel) this.checkPermissions<TextChannel>(
        *permission) else true

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

/**
 * The user's name and discriminator in the format `Username#Discriminator` (i.e. `User#0000`)
 */
val User.nameAndDiscrim
    get() = "${this.name}#${this.discriminator}"

/**
 * The member's name and discriminator int he format `Username#Discriminator` (.e. `User#0000`)
 */
val Member.nameAndDiscrim
    get() = this.user.nameAndDiscrim

val GuildChannel.hidden
    get() = this.getPermissionOverride(this.guild.publicRole)?.denied?.contains(Permission.MESSAGE_READ) ?: false