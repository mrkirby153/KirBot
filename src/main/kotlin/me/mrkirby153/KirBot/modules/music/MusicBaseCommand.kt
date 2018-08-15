package me.mrkirby153.KirBot.modules.music

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.utils.Context

abstract class MusicBaseCommand : BaseCommand(CommandCategory.MUSIC) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val manager = ModuleManager[MusicModule::class.java].getManager(context.guild)
        if (!manager.settings.enabled)
            return
        execute(context, cmdContext, manager)
    }

    abstract fun execute(context: Context, cmdContext: CommandContext, manager: MusicManager)
}