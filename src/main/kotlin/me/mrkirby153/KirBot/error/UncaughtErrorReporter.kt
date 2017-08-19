package me.mrkirby153.KirBot.error

import io.sentry.Sentry

class UncaughtErrorReporter : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (e == null)
            return
        e.printStackTrace()
        Sentry.getContext().apply {
            addTag("thread", t?.name ?: "Unknown Thread")
        }
        Sentry.capture(e)
        Sentry.getContext().clear()
    }
}