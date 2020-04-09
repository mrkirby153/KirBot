package me.mrkirby153.KirBot.modules

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.database.models.guild.ReactionRole
import me.mrkirby153.KirBot.event.Subscribe
import me.mrkirby153.KirBot.module.Module
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.utils.IdGenerator
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent

class ReactionRoles : Module("reaction-roles") {

    private val reactionRoleCache = CacheBuilder.newBuilder().maximumSize(100).build(object :
            CacheLoader<String, List<ReactionRole>>() {
        override fun load(key: String): List<ReactionRole> {
            return Model.where(ReactionRole::class.java, "message_id", key).get()
        }
    })

    private val idGenerator = IdGenerator(IdGenerator.ALPHA + IdGenerator.NUMBERS)

    override fun onLoad() {

    }

    @Subscribe
    fun onReactionAdd(event: MessageReactionAddEvent) {
        val member = event.member ?: return

        val rolesToGive = getRoles(event, member, false)

        if (rolesToGive.isNotEmpty())
            if (event.guild.selfMember.canInteract(member)) {
                debug("Giving ${member.user.nameAndDiscrim} ${rolesToGive.size} roles")
                rolesToGive.forEach {
                    event.guild.addRoleToMember(member, it).queue()
                }
            } else {
                debug("Can't give ${member.user.nameAndDiscrim} roles")
            }
    }


    @Subscribe
    fun onReactionRemove(event: MessageReactionRemoveEvent) {
        val member = event.member ?: return

        val rolesToTake = getRoles(event, member, true)

        if (rolesToTake.isNotEmpty())
            if (event.guild.selfMember.canInteract(member)) {
                debug("Taking ${member.user.nameAndDiscrim} ${rolesToTake.size} roles")
                rolesToTake.forEach {
                    event.guild.removeRoleFromMember(member, it).queue()
                }
            } else {
                debug("Can't take ${member.user.nameAndDiscrim} roles")
            }
    }

    @Subscribe
    fun onMessageDelete(event: MessageDeleteEvent) {
        // Clean up any reaction roles
        Model.where(ReactionRole::class.java, "message_id", event.messageId).delete()
    }

    fun addReactionRole(message: Message, role: Role, emote: String, custom: Boolean) {
        val rc = ReactionRole()
        rc.id = idGenerator.generate(10)
        rc.channelId = message.channel.id
        rc.messageId = message.id
        rc.role = role
        rc.emote = emote
        rc.custom = custom
        rc.save()

        // Invalidate the cache
        reactionRoleCache.invalidate(message.id)

        // Add our reaction to the message
        if (custom) {
            val resolvedEmote = message.guild.getEmoteById(emote) ?: return
            message.addReaction(resolvedEmote).queue()
        } else {
            message.addReaction(emote).queue()
        }
    }

    @Throws(IllegalArgumentException::class)
    fun removeReactionRole(id: String, guild: Guild) {
        val role = Model.where(ReactionRole::class.java, "id", id).first()
                ?: throw IllegalArgumentException("No reaction role with that id was found")
        if (role.guildId != guild.id)
            throw IllegalArgumentException("Reaction role not found on this guild")
        reactionRoleCache.invalidate(role.messageId)
        role.delete()
    }


    private fun getRoles(event: GenericMessageReactionEvent,
                         member: Member, has: Boolean): List<Role> {
        val roles = reactionRoleCache.get(event.messageId)

        val matchingReactionRoles = roles.filter { role -> if (event.reactionEmote.isEmote) role.emote == event.reactionEmote.emote.id else role.emote == event.reactionEmote.name }
        val rolesToGive = matchingReactionRoles.mapNotNull { it.role }.filter { if (has) it in member.roles else it !in member.roles }
        return rolesToGive
    }
}