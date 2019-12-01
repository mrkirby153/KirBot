package me.mrkirby153.KirBot.command.control

import com.mrkirby153.bfs.model.Model
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.PanelUser
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.HttpUtils
import me.mrkirby153.KirBot.utils.RED_TICK
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CompletableFuture


class CommandInvite {

    @Command(name = "guild-invite", arguments = ["<id:snowflake>"])
    @AdminCommand
    fun execute(context: Context, cmdContext: CommandContext) {
        val guild = Bot.shardManager.getGuildById(cmdContext.getNotNull<String>("id"))
                ?: throw CommandException("Guild not found")
        if (!guild.selfMember.hasPermission(Permission.CREATE_INSTANT_INVITE)) {
            throw CommandException("No permission to create invites")
        }

        context.channel.sendMessage(":timer: Adding you to **${guild.name}**").queue { msg ->
            tryOAuthJoin(context.author, guild).handle { result, throwable ->
                if (throwable == null) {
                    when (result) {
                        JoinResult.OK -> msg.editMessage("Added you to `${guild.name}`").queue()
                        JoinResult.ALREADY_IN -> msg.editMessage(
                                "You are already in `${guild.name}`").queue()
                        else -> msg.editMessage("An unknown error occurred").queue()
                    }
                } else {
                    msg.editMessage(buildString {
                        append("$RED_TICK Could not add you to `${guild.name}`: `${throwable.message}`")
                        if (throwable is OAuthJoinException) {
                            val body = throwable.response.body()
                            if (body != null) {
                                appendln("\n\n```${String(body.bytes())}```")
                            }
                        }
                    }).queue()
                }
            }
        }
    }

    /**
     * Attempts to join the user to a guild with oauth
     */
    private fun tryOAuthJoin(user: User, guild: Guild): CompletableFuture<JoinResult> {
        val future = CompletableFuture<JoinResult>()

        val panelUser = Model.where(PanelUser::class.java, "id", user.id).first()
        if (panelUser == null) {
            future.completeExceptionally(Exception("Panel user not found"))
            return future
        }
        val request = Request.Builder().apply {
            url("https://discordapp.com/api/v6/guilds/${guild.id}/members/${user.id}")
            val body = JSONObject()
            body.put("access_token", panelUser.token)
            header("Authorization", guild.jda.token)
            method("PUT", RequestBody.create(MediaType.parse("application/json"), body.toString()))
        }.build()

        Bot.LOG.debug("Making request: $request")

        HttpUtils.CLIENT.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                future.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                when (response.code()) {
                    204 -> future.complete(JoinResult.ALREADY_IN)
                    201 -> future.complete(JoinResult.OK)
                    else -> future.completeExceptionally(
                            OAuthJoinException("Unexpected error ${response.code()}", response))
                }
            }
        })

        return future
    }

    private enum class JoinResult {
        OK,
        ALREADY_IN
    }

    private class OAuthJoinException(msg: String, val response: Response) :
            java.lang.Exception(msg) {
    }
}