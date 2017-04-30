package me.mrkirby153.KirBot

import me.mrkirby153.KirBot.database.Database
import me.mrkirby153.KirBot.realname.RealnameHandler
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.server.ServerRepository
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.ListenerAdapter
import net.dv8tion.jda.core.link

class EventListener : ListenerAdapter() {


    override fun onMessageReceived(event: MessageReceivedEvent?) {
        if (event!!.author == Bot.jda.selfUser)
            return
        val server = ServerRepository.getServer(event.guild) ?: return
        server.handleMessageEvent(event)
    }

    override fun onGuildLeave(event: GuildLeaveEvent?) {
        val server = ServerRepository.getServer(event!!.guild!!) ?: return
        Database.onLeave(server)
        ServerRepository.servers.remove(event.guild.id)
    }

    override fun onGuildJoin(event: GuildJoinEvent?) {
        val server = ServerRepository.getServer(event!!.guild!!) ?: return
        Database.onJoin(server)
    }

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent?) {
        val server = ServerRepository.getServer(event!!.guild) ?: return
        notifyUserOfRealName(event.member, server)
    }

    private fun notifyUserOfRealName(member: Member, server: Server) {
        if (Database.requireRealname(server)) {
            server.publicChannel.send().text("Welcome to **${server.name}** ${member.asMention}! We required you to set your real name.").queue {
                server.publicChannel.send().embed("Real Name Required") {
                    field("", true, "Click here to set your real name" link Bot.properties.getProperty("realname-url"))
                }.rest().queue()
            }
            RealnameHandler(server).updateNames(true)
        }
    }
}