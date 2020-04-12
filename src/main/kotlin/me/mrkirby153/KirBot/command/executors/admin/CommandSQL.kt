package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.annotations.CommandDescription
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Database
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.checkPermissions
import me.mrkirby153.kcutils.Time
import me.mrkirby153.kcutils.use
import me.mrkirby153.kcutils.utils.TableBuilder
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import java.sql.SQLException
import java.util.concurrent.TimeoutException


class CommandSQL {

    @Command(name = "sql", arguments = ["<query:string...>"])
    @AdminCommand
    @CommandDescription("Execute raw SQL against the database")
    fun execute(context: Context, cmdContext: CommandContext) {
        val query = cmdContext.get<String>("query") ?: throw CommandException(
                "Please specify a query")

        val future = Bot.scheduler.submit {
            ModuleManager[Database::class].database.getConnection().use { con ->
                con.createStatement().use { statement ->
                    try {
                        val start_time = System.currentTimeMillis()
                        val r = statement.execute(query)
                        val end_time = System.currentTimeMillis()
                        if (r) {
                            val rs = statement.resultSet
                            val meta = rs.metaData
                            val columns = (1..meta.columnCount).map { meta.getColumnLabel(it) }

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
                                if(context.channel.checkPermissions(Permission.MESSAGE_ATTACH_FILES)) {
                                    context.channel.sendMessage(
                                            MessageBuilder("_Took ${Time.format(1,
                                                    end_time - start_time)}_").build()).addFile(
                                            table.toByteArray(), "query.txt").queue()
                                } else {
                                    context.send().error("Query is too long and files cannot be uploaded!").queue()
                                }
                            } else {
                                context.channel.sendMessage("```$table```_Took ${Time.format(1,
                                        end_time - start_time)}_").queue()
                            }
                        } else {
                            context.channel.sendMessage(
                                    ":ballot_box_with_check: ${statement.updateCount} row(s) updated").queue()
                        }
                    } catch (ex: SQLException) {
                        context.channel.sendMessage(":x: Error: ```${ex.message}```").queue()
                    }
                }
            }
        }
        // TODO 8/11/2019 Use completable future
        try {
            future.get()
        } catch (e: TimeoutException) {
            throw CommandException("Query took too long!")
        }
    }

}