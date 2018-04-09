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
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

            val proc = ProcessBuilder(dumpCommand.split(" ")).apply { redirectErrorStream(true) }.start()
            val inputStream = proc.inputStream

            val zip = File.createTempFile("mysql", ".sql.zip")
            val zos = ZipOutputStream(FileOutputStream(zip))
            val e = ZipEntry("$s.sql")
            zos.putNextEntry(e)

            Bot.LOG.debug("Executing: $dumpCommand")
            while (true) {
                val read = inputStream.read()
                if (read == -1)
                    break
                zos.write(read)
            }

            zos.closeEntry()
            zos.close()

            ModuleManager[AdminControl::class.java].logChannel?.sendFile(zip, "$s.sql.zip")?.queue{
                zip.delete() // Delete the file after we've sent it
            }
        } catch (e: Exception) {
           ModuleManager[AdminControl::class.java].logChannel?.sendMessage(":rotating_light: Database backup failed! `${e.message}`")?.queue()
        }

    }
}