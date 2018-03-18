package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.CommandCategory
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.database.models.Model
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Database
import me.mrkirby153.KirBot.user.CLEARANCE_GLOBAL_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import me.mrkirby153.kcutils.utils.TableBuilder
import java.sql.SQLException
import java.util.concurrent.TimeUnit

@Command(name = "sql", arguments = ["<query:string...>"], clearance = CLEARANCE_GLOBAL_ADMIN)
class CommandSQL : BaseCommand(false, CommandCategory.ADMIN) {
    override fun execute(context: Context, cmdContext: CommandContext) {
        val query = cmdContext.get<String>("query") ?: throw CommandException(
                "Please specify a query")

        Bot.scheduler.schedule({
            ModuleManager[Database::class].database.getConnection().use { con ->
                con.createStatement().use { statement ->
                    try {
                        val r = statement.execute(query)
                        if (r) {
                            val rs = statement.resultSet
                            val meta = rs.metaData
                            val columns = (1..meta.columnCount).map { meta.getColumnName(it) }

                            val builder = TableBuilder(columns.toTypedArray())

                            while (rs.next()) {
                                val data = mutableListOf<String?>()
                                columns.forEach {
                                    data.add(rs.getString(it))
                                }
                                builder.addRow(data.toTypedArray())
                            }

                            val table = builder.buildTable()
                            if (table.length > 1900) {
                                context.channel.sendFile(table.toByteArray(),
                                        "query-${Model.randomId()}.txt").queue()
                            } else {
                                context.channel.sendMessage("```$table```").queue()
                            }
                        } else {
                            context.channel.sendMessage(
                                    ":ballot_box_with_check: ${statement.updateCount} row(s) updated").queue()
                        }
                    } catch (ex: SQLException) {
                        context.channel.sendMessage(":x: Error: ```${ex.message}```").queue { msg ->
                            msg.deleteAfter(10, TimeUnit.SECONDS)
                            context.deleteAfter(10, TimeUnit.SECONDS)
                        }
                    }
                }
            }
        }, 0, TimeUnit.MILLISECONDS)
    }

}