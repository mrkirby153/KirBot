package me.mrkirby153.KirBot.command.executors.`fun`

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.HttpUtils
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONTokener
import java.net.URLEncoder

@Command(name = "urban", arguments = ["<term:string...>"])
class CommandUrban : BaseCommand(false) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val query = cmdContext.get<String>("term")!!

        val req = Request.Builder().url(
                "http://api.urbandictionary.com/v0/define?term=${URLEncoder.encode(query,
                        "UTF-8")}").build()

        val resp = HttpUtils.CLIENT.newCall(req).execute()

        val body = resp.body() ?: throw CommandException("Body is missing")

        val json = JSONObject(JSONTokener(body.string()))

        val resultType = json.getString("result_type")
        if (resultType == "no_results")
            throw CommandException("No definition found for `$query`")

        context.channel.sendMessage("$query - " +
                (json.getJSONArray("list")[0] as JSONObject).getString("definition")).queue()
    }
}