package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.backfill.BackfillJob
import me.mrkirby153.KirBot.backfill.BackfillManager
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.CommandException
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.kcutils.Time

@Command(name = "backfill", admin = true, arguments = ["<type:string>", "<id:snowflake>"])
class CommandBackfill : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        val type = cmdContext.getNotNull<String>("type")
        val id = cmdContext.getNotNull<String>("id")

        val backfillType = when (type.toLowerCase()) {
            "guild" -> BackfillJob.JobType.GUILD
            "channel" -> BackfillJob.JobType.CHANNEL
            "message" -> BackfillJob.JobType.MESSAGE
            else ->
                throw CommandException("Type not found")
        }
        val start = System.currentTimeMillis()
        val job = BackfillManager.backfill(context.guild, id, backfillType, onComplete = {
            context.channel.sendMessage(
                    "Backfill ${backfillType.name.toLowerCase().capitalize()} $id finished in ${Time.format(
                            1, System.currentTimeMillis() - start)}").queue()
        })
        context.channel.sendMessage(
                "Starting backfill of ${backfillType.name.toLowerCase().capitalize()} $id -- ${job.jobId}").queue()
    }

    @Command(name = "cancel", admin = true, arguments = ["<job:string>"])
    fun cancel(context: Context, cmdContext: CommandContext) {
        val job = cmdContext.getNotNull<String>("job")
        BackfillManager.cancel(job)
        context.send().success("Sent interrupt to $job").queue()
    }

    @Command(name = "status", admin = true)
    fun status(context: Context, cmdContext: CommandContext) {
        val jobs = BackfillManager.getRunningJobs()
        context.channel.sendMessage(buildString {
            appendln("**${jobs.size}** jobs are running")
            if (jobs.isNotEmpty()) {
                append("```")
                jobs.forEach {
                    appendln(" - ${it.jobId}: ${it.jobType} ${it.guild.id} ${it.id}")
                }
                append("```")
            }
        }).queue()
    }

    @Command(name = "logs", admin = true, arguments = ["<job:string>"])
    fun logs(context: Context, cmdContext: CommandContext) {
        val job = cmdContext.getNotNull<String>("job")
        val backfillJob = BackfillManager.getJob(job) ?: throw CommandException(
                "Job is not running")

        var msg = ""
        backfillJob.getLogMessages().forEach { m ->
            if (msg.length + m.length + 1 >= 1990) {
                context.channel.sendMessage(msg).queue()
                msg = "$m\n"
            } else {
                msg += "$m\n"
            }
        }
        context.channel.sendMessage(msg).queue()
    }
}