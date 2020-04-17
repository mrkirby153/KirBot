package me.mrkirby153.KirBot.command.executors.moderation

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.annotations.LogInModlogs
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.infraction.Infractions
import me.mrkirby153.KirBot.listener.WaitUtils
import me.mrkirby153.KirBot.user.CLEARANCE_MOD
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.canInteractWith
import me.mrkirby153.kcutils.utils.argparser.ArgumentParser
import me.mrkirby153.kcutils.utils.argparser.Option
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import java.util.concurrent.CompletableFuture
import javax.inject.Inject

class MassActionCommands @Inject constructor(private val infractions: Infractions){

    private val argumentParser = ArgumentParser().apply {
        addOption(Option("ids", required = true, help = "The ids to ban"))
        addOption(Option("--reason", aliases = arrayOf("-r"), help = "The reason", required = false,
                default = "No reason specified"))
    }

    private val ID_REGEX = Regex("\\d{17,18}")


    @Command(name = "mban", arguments = ["<options:string...>"],
            category = CommandCategory.MODERATION,
            permissions = [Permission.BAN_MEMBERS], clearance = CLEARANCE_MOD)
    @LogInModlogs
    @IgnoreWhitelist
    @CommandDescription("Mass bans users")
    fun mban(context: Context, cmdContext: CommandContext) {
        doMassAction(cmdContext, context, "ban") { id, guild, reason ->
            infractions.ban(id, guild, context.author.id, reason)
        }
    }

    @Command(name = "munban", arguments = ["<options:string...>"],
            category = CommandCategory.MODERATION, permissions = [Permission.BAN_MEMBERS], clearance = CLEARANCE_MOD)
    @LogInModlogs
    @IgnoreWhitelist
    @CommandDescription("Mass unban users")
    fun munban(context: Context, cmdContext: CommandContext) {
        doMassAction(cmdContext, context, "unban") { id, guild, reason ->
            infractions.unban(id, guild, context.author.id, reason)
        }
    }

    @Command(name = "mkick", arguments = ["<options:string...>"],
            category = CommandCategory.MODERATION, permissions = [Permission.KICK_MEMBERS], clearance = CLEARANCE_MOD)
    @LogInModlogs
    @IgnoreWhitelist
    @CommandDescription("Mass kick")
    fun mkick(context: Context, cmdContext: CommandContext) {
        doMassAction(cmdContext, context, "kick") { id, guild, reason ->
            infractions.kick(id, guild, context.author.id, reason)
        }
    }

    private fun doMassAction(cmdContext: CommandContext,
                             context: Context, actionName: String,
                             action: (String, Guild, String) -> CompletableFuture<*>) {
        val ids = try {
            getIdsAndReason(cmdContext.getNotNull("options"))
        } catch (e: java.lang.IllegalArgumentException) {
            throw CommandException(e.message)
        }
        context.channel.sendMessage(
                "Are you sure you want to ban ${ids.first.size} users with the reason: `${ids.second}`").queue { m ->
            WaitUtils.confirmYesNo(m, context.author, {
                m.editMessage("Processing...").queue()
                val results = performMassAction(ids.first,
                        context) { id, guild ->
                    action.invoke(id, guild, ids.second)
                }

                CompletableFuture.allOf(*results.third.toTypedArray()).thenAccept {
                    m.clearReactions().queue()
                    m.editMessage(
                            "${actionName.capitalize()}ed ${results.first.size} members. Could not $actionName ${results.second.size}").queue()
                }
            }, {
                m.clearReactions().queue()
                m.editMessage("Aborted!").queue()
            })
        }
    }


    private fun performMassAction(ids: List<String>, context: Context,
                                  function: (String, Guild) -> CompletableFuture<*>): Triple<List<String>, List<String>, List<CompletableFuture<*>>> {
        val successful = mutableListOf<String>()
        val failed = mutableListOf<String>()
        val futures = mutableListOf<CompletableFuture<*>>()

        ids.forEach { id ->
            val member = context.guild.getMemberById(id)
            if (member != null && (!context.guild.selfMember.canInteract(
                            member) || !context.author.canInteractWith(
                            context.guild, member.user))) {
                failed.add(id)
            } else {
                try {
                    Bot.LOG.debug("Performing mass action on $id")
                    val cf = function.invoke(id, context.guild)
                    successful.add(id)
                    futures.add(cf)
                } catch (e: Exception) {
                    failed.add(id)
                }
            }
        }
        return Triple(successful, failed, futures)
    }


    private fun getIdsAndReason(args: String): Pair<List<String>, String> {
        val parsed = argumentParser.parse(args)
        val ids = parsed["ids"]?.split(" ", ",", "|")
                ?: emptyList()
        if (ids.isEmpty())
            throw IllegalArgumentException("No IDs were provided")

        ids.forEach {
            if (!it.matches(ID_REGEX))
                throw IllegalArgumentException("$it is not a valid ID")
        }

        return Pair(ids, parsed.getOrDefault("reason", "No reason specified"));
    }
}