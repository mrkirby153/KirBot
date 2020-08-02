package com.mrkirby153.kirbot.services.command.context

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User

/**
 * Wrapper for the command sender to differentiate it from a normal user
 */
class CommandSender(val user: User) : User by user

/**
 * Wrapper for the current guild to differentiate it from a guild by its id
 */
class CurrentGuild(val guild: Guild) : Guild by guild

/**
 * Wrapper for the current channel to differentiate it from a channel
 */
class CurrentChannel(val channel: TextChannel) : TextChannel by channel

/**
 * Wrapper for [Member] that stores a nullable [member]. This is null if the provided id is not
 * in the guild. [user] may be null if the user is not known to the shard. [userId] will always
 * exist
 */
class OptionalMember(val member: Member?, val user: User?, val userId: String)

/**
 * Wrapper for [User] that stores the [user] if it is known to this ShardManager and its id
 */
class OptionalUser(val user: User?, val userId: String)