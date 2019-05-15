package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.getOrCreateOverride
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.TextChannel


private val lockdownMessages = mutableMapOf<String, String>()


class CommandLockdown {

    @Command(name = "lock", clearance = CLEARANCE_MOD, arguments = ["[msg:string...]"],
            category = CommandCategory.MODERATION)
    @LogInModlogs
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val msg = cmdContext.get<String>("msg") ?: "No reason given"

        val chan = context.channel as TextChannel

        val override = chan.getOrCreateOverride(context.guild.publicRole)

        if (Permission.MESSAGE_WRITE in override.denied) {
            throw CommandException("Channel is already locked")
        }

        chan.sendMessage(
                "Channel locked by **${context.author.nameAndDiscrim}**: $msg").queue { m ->
            lockdownMessages[chan.id] = m.id
            override.manager.deny(Permission.MESSAGE_WRITE).queue()
        }
    }
}


class CommandUnlock {
    @Command(name = "unlock", clearance = CLEARANCE_MOD, category = CommandCategory.MODERATION)
    @LogInModlogs
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val chan = context.channel as TextChannel

        val override = chan.getPermissionOverride(context.guild.publicRole)
                ?: throw CommandException("Channel isn't locked")

        if (Permission.MESSAGE_WRITE !in override.denied) {
            throw CommandException("Channel isn't locked")
        }

        override.manager.clear(Permission.MESSAGE_WRITE).queue {
            val m = lockdownMessages[chan.id]
            if (m != null) {
                context.channel.deleteMessageById(m).queue()
            }
            context.channel.sendMessage(
                    "Channel unlocked by **${context.author.nameAndDiscrim}**").queue()
        }
    }
}