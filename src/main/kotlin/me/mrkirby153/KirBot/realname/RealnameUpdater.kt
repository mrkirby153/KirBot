package me.mrkirby153.KirBot.realname

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.utils.kirbotGuild

class RealnameUpdater : Runnable {

    override fun run() {
        Bot.shardManager.shards
                .flatMap { it.guilds }
                .forEach { RealnameHandler(it.kirbotGuild).update() }
    }
}