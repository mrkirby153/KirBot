package me.mrkirby153.KirBot.command.executors.music

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.Argument
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.music.MusicManager
import me.mrkirby153.KirBot.utils.Context

abstract class MusicCommand(vararg arguments: Argument) : BaseCommand(CommandCategory.MUSIC, *arguments) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (MusicManager.musicSettings[context.guild.id]?.enabled == true) {
            exec(context, cmdContext)
        } else {
            Bot.LOG.debug("Music is disabled in ${context.guild.id}, ignoring")
        }
    }

    abstract fun exec(context: Context, cmdContext: CommandContext)
}