package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.GREEN_TICK
import me.mrkirby153.KirBot.utils.RED_TICK
import net.dv8tion.jda.core.Permission
import java.util.concurrent.TimeUnit

@Command(name = "guild-invite", arguments = ["<id:snowflake>"], admin = true)
class CommandInvite : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val guild = Bot.shardManager.getGuild(cmdContext.getNotNull("id"))
                ?: throw CommandException("Guild not found")
        if(!guild.selfMember.hasPermission(Permission.CREATE_INSTANT_INVITE)) {
            throw CommandException("No permission to create invites")
        }
        context.channel.sendMessage(":timer: Retrieving a single use invite...").queue { msg ->
            val chan = guild.defaultChannel
            if (chan == null) {
                msg.editMessage("$RED_TICK There is no default channel on the guild").queue()
                return@queue
            }
            chan.createInvite().setMaxAge(10, TimeUnit.MINUTES).setMaxUses(1).queue { inv ->
                msg.editMessage(
                        "$GREEN_TICK Invite generated: ${inv.url} . This invite is valid for the next 10 minutes").queue()
            }
        }
    }
}