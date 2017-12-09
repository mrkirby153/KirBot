package me.mrkirby153.KirBot.command.executors.game

import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.command.executors.CmdExecutor
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.HttpUtils
import me.mrkirby153.KirBot.utils.embed.link
import me.mrkirby153.KirBot.utils.round
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException

class CommandOverwatch : CmdExecutor() {

    val regions = arrayOf("us", "eu", "kr")


    override fun execute(context: Context, cmdContext: CommandContext) {
        var battleTag: String = cmdContext.string("battletag") ?: throw CommandException(
                "Invalid battle tag!")
        var region = cmdContext.string("region")

        if (region != null)
            if (region.toLowerCase() !in regions)
                throw CommandException("Invalid Region! Acceptable regions: `[us, eu, kr]`")

        battleTag = battleTag.replace("#", "-")

        context.channel.sendTyping().queue()

        val request = Request.Builder().url("https://owapi.net/api/v3/u/$battleTag/stats").build()

        val response = HttpUtils.CLIENT.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                response.use {
                    try {
                        val json = JSONObject(JSONTokener(response.body()!!.byteStream()))

                        var jso: JSONObject? = null
                        if (region != null) {
                            if (!json.has(region))
                                context.send().error(
                                        "Unable to find player `${battleTag.replace("-",
                                                "#")}` in region `${region?.capitalize()}`").queue()
                            jso = json.optJSONObject(region)
                        } else {
                            for (r in regions) {
                                if (!json.has(r))
                                    continue
                                jso = json.optJSONObject(r)
                                region = r
                                break
                            }
                        }

                        context.send().embed("Overwatch Stats for ${battleTag.replace("-", "#")}") {
                            description {
                                +("Battle Tag: **" + (battleTag.replace("-",
                                        "#") link "https://playoverwatch.com/en-gb/career/pc/$region/$battleTag") + "**")
                                +"\nRegion: **[${region?.toUpperCase()}]()**"
                            }

                            val overall = jso?.optJSONObject("stats")
                            if (overall == null) {
                                context.send().error(
                                        "Unable to find stats for player `${battleTag.replace("-",
                                                "#")}`").queue()
                                return
                            }
                            overall.getJSONObject("quickplay")?.optJSONObject(
                                    "overall_stats")?.let {
                                thumbnail = it.optString("avatar")
                            }
                            val lvl = overall.optJSONObject("quickplay")?.getJSONObject(
                                    "overall_stats")?.getInt("level") ?: 0
                            val prestige = overall.optJSONObject("quickplay")?.getJSONObject(
                                    "overall_stats")?.getInt("prestige") ?: 0

                            fields {
                                field {
                                    title = "General"
                                    description = "Level: [${prestige * 100 + lvl}]()"
                                }
                                overall.getJSONObject("quickplay")?.optJSONObject(
                                        "game_stats")?.let {
                                    field {
                                        title = "Quick Play"
                                        inline = true
                                        description {
                                            appendln("K/D Ratio: **[${it.optDouble("kpd")}]()**")
                                            appendln("Time Played: **[${it.optDouble(
                                                    "time_played")} hours]()**")
                                        }
                                    }
                                }
                                overall.optJSONObject("competitive")?.let {
                                    field {
                                        title = "Competitive"
                                        inline = true
                                        description {
                                            it.optJSONObject("game_stats")?.let {
                                                val competitiveGamesPlayed = it.optLong(
                                                        "games_played")
                                                appendln("Avg. Elims: **[${(it.optDouble(
                                                        "eliminations") / competitiveGamesPlayed).round(
                                                        2)}]()**")
                                                appendln("Avg. Deaths: **[${(it.optDouble(
                                                        "deaths") / competitiveGamesPlayed).round(
                                                        2)}]()**")
                                                appendln(
                                                        "K/D Ratio: **[${it.optDouble("kpd")}]()**")
                                                appendln("Time Played: **[${it.optDouble(
                                                        "time_played")} hours]()**")
                                            }
                                            it.optJSONObject("overall_stats")?.let {
                                                appendln("Wins/Ties/Losses: **[${it.optInt(
                                                        "wins")}]()** | **[${it.optInt(
                                                        "ties")}]()** | **[${it.optInt(
                                                        "losses")}]()**")
                                                val rank = it.optInt("comprank")
                                                var displayRank = ""
                                                if (rank == 0)
                                                    displayRank = "Unranked"
                                                else if (rank < 1499)
                                                    displayRank = "Bronze"
                                                else if (rank in 1500..1999)
                                                    displayRank = "Silver"
                                                else if (rank in 2000..2499)
                                                    displayRank = "Gold"
                                                else if (rank in 2500..2999)
                                                    displayRank = "Platinum"
                                                else if (rank in 3000..3499)
                                                    displayRank = "Diamond"
                                                else if (rank in 3500..3999)
                                                    displayRank = "Master"
                                                else if (rank >= 4000)
                                                    displayRank = "Grandmaster"
                                                appendln(
                                                        "Rank: **${it.optInt(
                                                                "comprank")}** ($displayRank)")
                                            }
                                        }
                                    }

                                }
                            }
                        }.rest().queue()
                    } catch (e: Exception) {
                        throw CommandException("An error occurred when looking up that player")
                    }
                }
            }

            override fun onFailure(call: Call, response: IOException) {
                call.cancel()
                response.printStackTrace(System.out)
                throw CommandException("Could not query API")
            }
        })
    }
}