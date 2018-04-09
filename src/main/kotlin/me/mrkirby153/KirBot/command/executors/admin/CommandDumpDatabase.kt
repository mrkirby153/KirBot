package me.mrkirby153.KirBot.command.executors.admin

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.AdminControl
import me.mrkirby153.KirBot.user.CLEARANCE_GLOBAL_ADMIN
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.nameAndDiscrim
import me.mrkirby153.kcutils.Time
import java.io.File
import java.io.PrintStream
import java.text.SimpleDateFormat

@Command(name = "db-backup", clearance = CLEARANCE_GLOBAL_ADMIN)
class CommandDumpDatabase : BaseCommand(false) {

    override fun execute(context: Context, cmdContext: CommandContext) {
        Bot.LOG.info(
                "Taking Database Dump (Requested by ${context.author.nameAndDiscrim} [${context.author.id}])")

        ModuleManager[AdminControl::class.java].logChannel?.sendMessage(":warning: Database backup triggered by ${context.author.nameAndDiscrim}")?.queue()
        try {
            val dumpCommand = "mysqldump ${Bot.properties.getProperty(
                    "database")} -h ${Bot.properties.getProperty(
                    "database-host")} --user=${Bot.properties.getProperty("database-username")} --password=${Bot.properties.getProperty("database-password")}"

            val s = "mysqldump-${SimpleDateFormat(Time.DATE_FORMAT_NOW).format(
                    System.currentTimeMillis())}"
            val out = File.createTempFile(s, ".sql")

            val proc = ProcessBuilder(dumpCommand.split(" ")).apply { redirectErrorStream(true) }.start()
            val ps = PrintStream(out)
            val inputStream = proc.inputStream

            Bot.LOG.debug("Executing: $dumpCommand")
            while (true) {
                val read = inputStream.read()
                if (read == -1)
                    break
                ps.write(read)
            }

            ps.flush()

            ModuleManager[AdminControl::class.java].logChannel?.sendFile(out, "$s.sql")?.queue{
                out.delete() // Delete the file after we've sent it
            }
        } catch (e: Exception) {
           ModuleManager[AdminControl::class.java].logChannel?.sendMessage(":rotating_light: Database backup failed! `${e.message}`")?.queue()
        }

    }
}