package me.mrkirby153.KirBot.realname

import me.mrkirby153.KirBot.Bot

class RealnameUpdater : Runnable {

    override fun run() {
        for(shard in Bot.shardManager.shards){
            for(guild in shard.guilds){
                RealnameHandler(guild, shard.getServerData(guild)).updateNames(true)
            }
        }
    }
}