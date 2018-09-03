package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.guild.MusicSettings
import me.mrkirby153.KirBot.google.YoutubeSearch
import me.mrkirby153.KirBot.modules.music.MusicBaseCommand
import me.mrkirby153.KirBot.modules.music.MusicManager
import me.mrkirby153.KirBot.modules.music.TrackLoader
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.checkPermissions
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel

@Command(name = "play", arguments = ["[query/url:string...]"],
        permissions = [Permission.MESSAGE_EMBED_LINKS])
@CommandDescription("Play music")
class PlayCommand : MusicBaseCommand() {
    override fun execute(context: Context, cmdContext: CommandContext, manager: MusicManager) {
        val data = cmdContext.get<String>("query/url")

        if (data == null) {
            if (manager.playing)
                throw CommandException("Already playing!")
            manager.resume()
            context.channel.sendMessage(":arrow_forward: Music resumed!").queue()
            return
        }
        if (!context.member.voiceState.inVoiceChannel())
            throw CommandException("Please join a voice channel first!")

        if (context.guild.selfMember.voiceState.inVoiceChannel()) {
            if (context.member.voiceState.channel != context.guild.selfMember.voiceState.channel)
                throw CommandException(
                        "I am already playing music in **${context.guild.selfMember.voiceState.channel.name}**. Join me there or use `${cmdPrefix}summon` to summon me to your current channel")
        }
        when (manager.settings.whitelistMode) {
            MusicSettings.WhitelistMode.WHITELIST -> {
                if (context.guild.selfMember.voiceState.channel.id !in manager.settings.channels)
                    throw CommandException("I cannot play music in your channel")
            }
            MusicSettings.WhitelistMode.BLACKLIST -> {
                if (context.guild.selfMember.voiceState.channel.id in manager.settings.channels)
                    throw CommandException("I cannot play music in your channel")
            }
            MusicSettings.WhitelistMode.OFF -> {
                // Do nothing, whitelisting is disabled
            }
        }

        if (!context.member.voiceState.channel.checkPermissions(Permission.VOICE_CONNECT))
            throw CommandException("I cannot join your voice channel!")

        if (!context.guild.selfMember.voiceState.inVoiceChannel()) {
            manager.connect(context.member.voiceState.channel, context.channel as TextChannel)
        }

        var url = data.replace(Regex("&list=[A-Za-z0-9\\-+_]+"),
                "") // Remove the YT List stuff from the URL
        val msg = context.channel.sendMessage(":timer: Loading `$url` please wait").complete()
        val ytSearch = !url.matches(Regex("^https?://.*"))
        if (ytSearch) {
            url = YoutubeSearch(url).execute()
        }
        if (manager.settings.maxQueueLength != -1 && manager.queueLength() / (60 * 1000) >= manager.settings.maxQueueLength) {
            throw CommandException(
                    "The queue is too long right now, please try again when it is shorter")
        }
        Bot.playerManager.loadItem(url, TrackLoader(manager, context, msg))
    }
}