package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.IgnoreWhitelist
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.HttpUtils
import me.mrkirby153.KirBot.utils.escapeMentions
import me.mrkirby153.KirBot.utils.toTypedArray
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder


class CommandUrban {

    @Command(name = "urban", arguments = ["<term:string...>"], category = CommandCategory.FUN)
    @CommandDescription("Retrieve definitions of words from the Urban Dictionary")
    @IgnoreWhitelist
    fun execute(context: Context, cmdContext: CommandContext) {
        val query = cmdContext.get<String>("term")!!

        val req = Request.Builder().url(
                "http://api.urbandictionary.com/v0/define?term=${URLEncoder.encode(query,
                        "UTF-8")}").build()

        val resp = HttpUtils.CLIENT.newCall(req).execute()

        val body = resp.body() ?: throw CommandException("Body is missing")

        val json = JSONObject(JSONTokener(body.string()))

        val results = json.getJSONArray("list").toTypedArray(JSONObject::class.java)
        if (results.isEmpty())
            throw CommandException("No definition for `$query`")
        context.channel.sendMessage(
                "$query - ${results.first().getString("definition").replace(Regex("\\[([^]]+)]"),
                        "$1")}".escapeMentions()).queue()
    }
}