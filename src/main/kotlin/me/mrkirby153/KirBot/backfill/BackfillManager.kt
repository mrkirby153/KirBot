package me.mrkirby153.KirBot.backfill

import me.mrkirby153.KirBot.Bot
import net.dv8tion.jda.core.entities.Guild
import java.util.concurrent.ConcurrentHashMap

object BackfillManager {

    private val runningBackfillTasks = ConcurrentHashMap<String, BackfillJob>()

    fun backfill(guild: Guild, id: String, type: BackfillJob.JobType,
                 maxMessages: Long = -1, onComplete: (() -> Unit)? = null): BackfillJob {
        val runnable = BackfillJob(Bot.idGenerator.generate().toString(), guild, id, type,
                maxMessages)
        val thread = Thread(runnable)
        thread.name = "Backfill/${guild.id}/$type/$id"
        thread.isDaemon = true
        runnable.thread = thread

        this.runningBackfillTasks[runnable.jobId] = runnable
        runnable.onComplete = {
            Bot.LOG.debug("Backfill task ${runnable.jobId} has finished")
            this.runningBackfillTasks.remove(runnable.jobId)
            onComplete?.invoke()
        }
        Bot.LOG.debug("Starting $type backfill on $guild/$id with id ${runnable.jobId}")
        thread.start()
        return runnable
    }


    fun cancel(id: String) {
        val job = runningBackfillTasks.remove(id) ?: return
        job.thread.interrupt()
    }

    fun getJob(id: String) = this.runningBackfillTasks[id]

    fun getRunningJobs() = this.runningBackfillTasks.values
}