package com.mrkirby153.kirbot.services.command.context

import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User

/**
 * Wrapper for the command sender to differentiate it from a normal user
 */
class CommandSender(user: User) : User by user

/**
 * Wrapper for the current guild to differentiate it from a guild by its id
 */
class CurrentGuild(guild: Guild) : Guild by guild