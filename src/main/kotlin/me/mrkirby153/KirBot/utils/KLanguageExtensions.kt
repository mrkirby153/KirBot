package me.mrkirby153.KirBot.utils

import com.mrkirby153.bfs.model.Model
import com.mrkirby153.bfs.sql.elements.Pair
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.server.KirBotGuild
import me.mrkirby153.KirBot.sharding.Shard
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import net.dv8tion.jda.core.entities.PermissionOverride
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import org.json.JSONArray
import java.io.InputStream
import java.util.Properties
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

// ----- Method Extensions -----
/**
 * Reads an [InputStream] into a [Properties] object
 *
 * @return The properties
 */
fun InputStream.readProperties(): Properties {
    return Properties().apply { load(this@readProperties) }
}

/**
 * Gets a [User's][User] clearance in a [Guild]
 *
 * Clearance ranges from 0 to 100 with higher numbers equaling higher clearance
 *
 * @param guild The guild to check clearance in
 *
 * @return The user's clearance.
 */
fun User.getClearance(guild: Guild) = guild.kirbotGuild.getClearance(this)

/**
 * Gets the user as a [Guild Member][Member] on the given guild
 *
 * @param guild The guild to get the member on
 *
 * @return A [Member] or null if the user isn't in the guild
 */
fun User.getMember(guild: Guild): Member? = guild.getMember(this)

/**
 * Checks if the user can interact with the given user in a guild
 *
 * Users can interact with another user if and only if they have a higher [clearance][getClearance] than
 * the other user
 *
 * @param guild The guild to check in
 * @param user The user to interact with
 *
 * @return True if the user can interact with the other user
 */
fun User.canInteractWith(guild: Guild, user: User): Boolean = this.getClearance(
        guild) > user.getClearance(guild)

/**
 * Gets the [Shard] that represents the guild
 *
 * @return The [Shard] or null if the shard wasn't found
 */
fun Guild.shard(): Shard? {
    return Bot.shardManager.getShard(this)
}

/**
 * Gets a permission override for a [Member] or creates it if it doesn't exist
 *
 * @return The permission override
 */
fun Channel.getOrCreateOverride(member: Member): PermissionOverride {
    return this.getPermissionOverride(member) ?: this.createPermissionOverride(member).complete()
}

/**
 * Gets a permission override for a [Role] or creates it if it doesn't exist
 *
 * @return The permission override
 */
fun Channel.getOrCreateOverride(role: Role): PermissionOverride {
    return this.getPermissionOverride(role) ?: this.createPermissionOverride(role).complete()
}

/**
 * Hides a text channel from everyone except the given users and roles
 *
 * @param exceptUsers A list of users that can still view the channel after hiding it
 * @param exceptRoles A list of roles that can still view the channel after hiding it
 */
fun TextChannel.hide(exceptUsers: List<User> = emptyList(), exceptRoles: List<Role> = emptyList()) {
    this.permissionOverrides.filter {
        it.allowed.contains(Permission.MESSAGE_READ)
    }.forEach { override ->
        if (override.isMemberOverride && override.member == this.guild.selfMember
                || override.member.user in exceptUsers)
            return@forEach
        if (override.isRoleOverride && override.role in exceptRoles)
            return@forEach

        override.manager.clear(Permission.MESSAGE_READ).queue()
    }
    val userOverride = this.getOrCreateOverride(this.guild.selfMember)
    userOverride.manager.grant(Permission.MESSAGE_READ).queue()
    val public = this.getOrCreateOverride(this.guild.publicRole)
    public.manager.deny(Permission.MESSAGE_READ).queue()

    exceptRoles.forEach {
        this.getOrCreateOverride(it).manager.grant(Permission.MESSAGE_READ).queue()
    }
    exceptUsers.forEach {
        it.getMember(guild)?.run {
            this@hide.getOrCreateOverride(this).manager.grant(Permission.MESSAGE_READ).queue()
        }
    }
    Model.query(me.mrkirby153.KirBot.database.models.Channel::class.java).where("id",
            this.id).update(Pair("hidden", true))
}

/**
 * Unhides the channel
 */
fun TextChannel.unhide() {
    val public = this.getPermissionOverride(this.guild.publicRole) ?: return
    public.manager.clear(Permission.MESSAGE_READ).queue()
    Model.query(me.mrkirby153.KirBot.database.models.Channel::class.java).where("id",
            this.id).update(Pair("hidden", false))
}

/**
 * Sanitizes (escapes markdown and mentions) a string
 *
 * @return A copy of the string with markdown escaped and mentions sanitized
 */
fun String.sanitize(): String {
    return this.escapeMarkdown().escapeMentions()
}

/**
 * Escapes markdown in a string
 *
 * @return A copy of the string with all the markdown escaped
 */
fun String.escapeMarkdown(): String {
    return this.replace(Regex("([*\\[\\]_()~])"), "\\\\$1").replace("`", "ˋ")
}

/**
 * Escapes all URL's embeds in the string
 *
 * @return A copy of the string with URLs escaped
 */
fun String.urlEscape(): String {
    return this.replace(Regex("(<)(https?://\\S+)(>)"), "$2").replace(Regex("https?://\\S+"),
            "<$0>")
}

/**
 * Deletes a message after the given time
 *
 * @param time The amount of time
 * @param unit The time units
 *
 * @return A scheduled future of the task, or null if permissions are lacking
 */
fun Message.deleteAfter(time: Long, unit: TimeUnit): ScheduledFuture<*>? {
    if (this.channel.checkPermissions(Permission.MESSAGE_MANAGE))
        return this.delete().queueAfter(time, unit)
    return null
}

/**
 * Checks if the bot has the given permissions on a channel
 *
 * @param permission The permissions to check
 *
 * @return True if the bot has all permissions, false if otherwise
 */
fun <T : Channel> T.checkPermissions(
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
 * Escapes all mentions in a string by replacing @'s with @ and a Zero Width Space
 *
 * @return The string with all mentions escaped
 */
fun String.escapeMentions(): String {
    // Replace all @ symbols with @ followed by a Zero-Width Space
    return this.replace("@", "@\u200B")
}

/**
 * Removes a reaction made by a user
 *
 * @param user The user to remove the reaction from
 * @param reaction The reaction to remove
 */
fun Message.removeReaction(user: User, reaction: String) {
    Bot.LOG.debug("Removing $reaction from $id")
    this.reactions.filter {
        Bot.LOG.debug("Reaction: ${it.reactionEmote.name}")
        it.reactionEmote.name == reaction
    }.forEach {
        it.removeReaction(user).queue()
    }
}

/**
 * Returns the JSONArray as a typed array
 *
 * @param clazz The class to cast elements in the array to
 */
fun <T> JSONArray.toTypedArray(clazz: Class<T>): List<T> {
    return this.map { it as T }
}

/**
 * Checks if the string is a number
 *
 * @return True if it is a number, false if it isn't
 */
fun String.isNumber(): Boolean {
    try {
        this.toDouble()
    } catch (e: NumberFormatException) {
        return false
    }
    return true
}

/**
 * Checks if this member can assign the given role to a user
 *
 * @param role The role to check
 *
 * @return True if the user can assign the role, false if they can't
 */
fun Member.canAssign(role: Role): Boolean {
    if (!this.hasPermission(net.dv8tion.jda.core.Permission.MANAGE_ROLES))
        return false
    val memberHighest = this.roles.map { it.position }.max() ?: 0
    return memberHighest > role.position
}

/**
 * Resolve mentions (Both role and user) in a string
 *
 * @return A string with the mentions resolved
 */
fun String.resolveMentions(): String {
    return this.resolveRoles().resolveUserMentions()
}

/**
 * Resolve all the role mentions in a string
 *
 * @return A string with role mentions resolved
 */
fun String.resolveRoles(): String {
    val roleList = Bot.shardManager.shards.flatMap { it.roles }
    val regex = Regex("<@&(\\d{17,18})>")
    var mutableMsg = this
    var matched: Boolean
    do {
        val matcher = regex.find(mutableMsg)
        if (matcher == null) {
            matched = false
        } else {
            matched = true
            val roleId = matcher.groupValues[1]
            val role = roleList.firstOrNull { it.id == roleId }
            val name = role?.name ?: "invalid-role"
            mutableMsg = mutableMsg.replace("<@&$roleId>", "@$name")
        }
    } while (matched)
    return mutableMsg
}

/**
 * Resolve all user mentions in a string
 *
 * @return A string with the user mentions resolved
 */
fun String.resolveUserMentions(): String {
    val members = Bot.shardManager.shards.flatMap { it.users }
    val regex = Regex("<@!?(\\d{17,18})>")
    var mutableMsg = this
    var matched: Boolean
    do {
        val matcher = regex.find(mutableMsg)
        if (matcher == null) {
            matched = false
        } else {
            matched = true
            val memberId = matcher.groupValues[1]
            val member = members.firstOrNull { it.id == memberId }
            val name = member?.nameAndDiscrim ?: "invalid-user"
            mutableMsg = mutableMsg.replace(Regex("<@!?$memberId>"), "@$name")
        }
    } while (matched)
    return mutableMsg
}

/**
 * Gets the user's online status
 *
 * @return The user's online status
 */
fun User.getOnlineStats(): OnlineStatus {
    val guild = this.mutualGuilds.firstOrNull() ?: return OnlineStatus.OFFLINE
    val m = this.getMember(guild) ?: return OnlineStatus.OFFLINE
    return m.onlineStatus
}

// ----- Property Extensions -----

/**
 * If the [User] is a global admin
 */
val User.globalAdmin: Boolean
    get() = ModuleManager.getLoadedModule(Redis::class.java)?.getConnection()?.use {
        it.sismember("admins", this.id)
    } ?: false

/**
 * The Guild's [KirBotGuild]
 */
val Guild.kirbotGuild: KirBotGuild
    get() = KirBotGuild[this]

/**
 * The user's username and discriminator concatenated
 */
val User.nameAndDiscrim
    get() = this.name + "#" + this.discriminator

/**
 * The user's name for logging in modlogs
 */
val User.logName
    get() = "${this.name.sanitize()}#${this.discriminator.sanitize()} (`${this.id}`)"