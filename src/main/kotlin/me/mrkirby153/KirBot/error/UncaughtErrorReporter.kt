package me.mrkirby153.KirBot.error

import io.sentry.Sentry
import me.mrkirby153.KirBot.Bot

class UncaughtErrorReporter : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (e == null)
            return
        Bot.LOG.error("Encountered an uncaught exception", e)
        Sentry.capture(e)
    }
}