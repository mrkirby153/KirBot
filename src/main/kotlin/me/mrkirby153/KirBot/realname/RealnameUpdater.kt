package me.mrkirby153.KirBot.realname

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.server.ServerRepository

class RealnameUpdater : Runnable {

    override fun run() {
        Bot.jda.guilds
                .mapNotNull { ServerRepository.getServer(it) }
                .forEach { RealnameHandler(it).updateNames(true) }
    }
}