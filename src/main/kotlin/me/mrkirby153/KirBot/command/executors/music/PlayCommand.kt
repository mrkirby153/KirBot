package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.google.YoutubeSearch
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.music.MusicModule
import me.mrkirby153.KirBot.modules.music.TrackLoader
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.settings.GuildSettings
import me.mrkirby153.KirBot.utils.toTypedArray
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.TextChannel
import javax.inject.Inject


class PlayCommand @Inject constructor(private val musicModule: MusicModule){
    @Command(name = "play", arguments = ["[query/url:string...]"], category = CommandCategory.MUSIC)
    @CommandDescription("Play music")
    fun execute(context: Context, cmdContext: CommandContext) {
        if (!GuildSettings.musicEnabled.get(context.guild))
            return
        val manager = musicModule.getManager(context.guild)
        val cmdPrefix = GuildSettings.commandPrefix.get(context.guild)
        val data = cmdContext.get<String>("query/url")

        if (data == null) {
            if (manager.playing)
                throw CommandException("Already playing!")
            manager.resume()
            context.channel.sendMessage(":arrow_forward: Music resumed!").queue()
            return
        }
        val member = context.member ?: throw CommandException("Member not found.")
        val voiceState = member.voiceState ?: throw CommandException("Please join a voice channel first")
        if (!voiceState.inVoiceChannel())
            throw CommandException("Please join a voice channel first!")

        if (context.guild.selfMember.voiceState!!.inVoiceChannel()) {
            if (voiceState.channel != context.guild.selfMember.voiceState!!.channel)
                throw CommandException(
                        "I am already playing music in **${context.guild.selfMember.voiceState!!.channel!!.name}**. Join me there or use `${cmdPrefix}summon` to summon me to your current channel")
        }
        val whitelistChannels = GuildSettings.musicChannels.get(context.guild).toTypedArray(String::class.java)
        when (GuildSettings.musicWhitelistMode.get(context.guild)) {
            "WHITELIST" -> {
                if (voiceState.channel!!.id !in whitelistChannels)
                    throw CommandException("I cannot play music in your channel")
            }
            "BLACKLIST" -> {
                if (voiceState.channel!!.id in whitelistChannels)
                    throw CommandException("I cannot play music in your channel")
            }
            "OFF" -> {
                // Do nothing, whitelisting is disabled
            }
            else -> {
                throw CommandException("Whitelist is in an unknown state!")
            }
        }

        if (voiceState.channel?.checkPermissions(Permission.VOICE_CONNECT) == false)
            throw CommandException("I cannot join your voice channel!")

        if (!context.guild.selfMember.voiceState!!.inVoiceChannel()) {
            manager.connect(voiceState.channel!!, context.channel as TextChannel)
        }

        var url = data.replace(Regex("&list=[A-Za-z0-9\\-+_]+"),
                "") // Remove the YT List stuff from the URL
        val msg = context.channel.sendMessage(":timer: Loading `$url` please wait").complete()
        val ytSearch = !url.matches(Regex("^https?://.*"))
        if (ytSearch) {
            val searchResult = YoutubeSearch(url).execute()
            if(searchResult == null) {
                msg.editMessage(":warning: No results returned for `$url`").queue()
                return
            }
            url = searchResult
        }
        val maxQueueLength = GuildSettings.musicMaxQueueLength.get(context.guild)
        if (maxQueueLength != -1L && manager.queueLength() / (60 * 1000) >= maxQueueLength) {
            throw CommandException(
                    "The queue is too long right now, please try again when it is shorter")
        }
        Bot.playerManager.loadItem(url, TrackLoader(manager, context, msg))
    }
}