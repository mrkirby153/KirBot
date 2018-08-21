package me.mrkirby153.KirBot.modules.music

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getClearance
import net.dv8tion.jda.core.entities.Member

abstract class MusicBaseCommand : BaseCommand(CommandCategory.MUSIC) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val manager = ModuleManager[MusicModule::class.java].getManager(context.guild)
        if (!manager.settings.enabled)
            return
        execute(context, cmdContext, manager)
    }

    abstract fun execute(context: Context, cmdContext: CommandContext, manager: MusicManager)

    fun isDJ(member: Member): Boolean {
        if (member.user.getClearance(member.guild) > CLEARANCE_MOD)
            return true
        return member.roles.map { it.name }.firstOrNull { it.equals("DJ", true) } != null
    }

    fun alone(member: Member): Boolean {
        if (!member.voiceState.inVoiceChannel())
            return false
        return member.voiceState.channel.members.filter { it != member.guild.selfMember }.size == 1
    }
}