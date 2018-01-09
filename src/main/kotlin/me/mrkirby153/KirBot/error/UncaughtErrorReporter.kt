package me.mrkirby153.KirBot.error

import me.mrkirby153.KirBot.logger.ErrorLogger

class UncaughtErrorReporter : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (e == null)
            return
        e.printStackTrace()
        ErrorLogger.logThrowable(e)
    }
}