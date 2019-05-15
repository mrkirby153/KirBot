package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.google.YoutubeSearch
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.music.MusicModule
import me.mrkirby153.KirBot.modules.music.TrackLoader
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.SettingsRepository
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.KirBot.utils.toTypedArray
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel
import org.json.JSONArray


class PlayCommand {
    @Command(name = "play", arguments = ["[query/url:string...]"],
            permissions = [Permission.MESSAGE_EMBED_LINKS], category = CommandCategory.MUSIC)
    @CommandDescription("Play music")
    fun execute(context: Context, cmdContext: CommandContext) {
        val manager = ModuleManager[MusicModule::class.java].getManager(context.guild)
        if (SettingsRepository.get(context.guild, "music_enabled", "0") == "0")
            return
        val cmdPrefix = SettingsRepository.get(context.guild, "cmd_prefix", "!")
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
        val whitelistChannels = SettingsRepository.getAsJsonArray(context.guild, "music_channels",
                JSONArray())!!.toTypedArray(String::class.java)
        when (SettingsRepository.get(context.guild, "music_mode", "OFF")) {
            "WHITELIST" -> {
                if (context.member.voiceState.channel.id !in whitelistChannels)
                    throw CommandException("I cannot play music in your channel")
            }
            "BLACKLIST" -> {
                if (context.member.voiceState.channel.id in whitelistChannels)
                    throw CommandException("I cannot play music in your channel")
            }
            "OFF" -> {
                // Do nothing, whitelisting is disabled
            }
            else -> {
                throw CommandException("Whitelist is in an unknown state!")
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
        val maxQueueLength = SettingsRepository.get(context.guild, "music_max_queue_length",
                "-1")!!.toInt()
        if (maxQueueLength != -1 && manager.queueLength() / (60 * 1000) >= maxQueueLength) {
            throw CommandException(
                    "The queue is too long right now, please try again when it is shorter")
        }
        Bot.playerManager.loadItem(url, TrackLoader(manager, context, msg))
    }
}