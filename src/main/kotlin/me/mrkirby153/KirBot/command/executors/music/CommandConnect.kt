package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.modules.music.MusicBaseCommand
import me.mrkirby153.KirBot.modules.music.MusicManager
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.checkPermissions
import me.xdrop.fuzzywuzzy.FuzzySearch
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Channel
import net.dv8tion.jda.core.entities.VoiceChannel

@Command(name = "connect,summon", arguments = ["[channel:string...]"])
class CommandConnect : MusicBaseCommand() {
    
    override fun execute(context: Context, cmdContext: CommandContext, manager: MusicManager) {
        val chanName = cmdContext.get<String>("channel")
        val channel = if (chanName != null) fuzzyMatchChannel(chanName,
                context.guild.voiceChannels) as VoiceChannel else context.member.voiceState.channel
                ?: throw CommandException("Please join a voice channel first!")
        if (!channel.checkPermissions(Permission.VOICE_CONNECT))
            throw CommandException("I cannot join this voice channel")

        if(context.guild.selfMember.voiceState.inVoiceChannel()){
            if(context.guild.selfMember.voiceState.channel.members.any { it != context.guild.selfMember }){
                if(!isDJ(context.member))
                    throw CommandException("Only DJs can switch channels while the bot is playing")
            }
        }

        manager.connect(channel, context.textChannel)
        context.send().success(
                "Joining **${channel.name}** and binding to <#${context.channel.id}>").queue()
    }

    private fun fuzzyMatchChannel(query: String, channels: List<Channel>): Channel? {
        if (query.matches(Regex("\\d{17,18}"))) {
            return channels.first { it.id == query }
        }
        val exactMatches = channels.filter {
            it.name.toLowerCase().replace(" ", "") == query.replace(" ", "").toLowerCase()
        }
        if (exactMatches.isNotEmpty()) {
            if (exactMatches.size > 1)
                throw CommandException("Too many matches for the query `$query`")
            return exactMatches.first()
        } else {
            val fuzzyRated = mutableMapOf<Channel, Int>()
            channels.forEach { chan ->
                fuzzyRated[chan] = FuzzySearch.partialRatio(query.toLowerCase().replace(" ", ""),
                        chan.name.toLowerCase().replace(" ", ""))
            }
            if (fuzzyRated.isEmpty())
                return null
            val entries = fuzzyRated.entries.sortedBy { it.value }.reversed().filter { it.value > 40 }
            val first = entries.first()
            return when {
                entries.size == 1 -> first.key
                first.value - entries[1].value > 20 -> first.key
                else -> throw CommandException("Too many matches for the query `$query`")
            }
        }
    }
}