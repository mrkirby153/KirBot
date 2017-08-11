package me.mrkirby153.KirBot.error

import me.mrkirby153.KirBot.Bot

class UncaughtErrorReporter: Thread.UncaughtExceptionHandler {

    override fun uncaughtException(t: Thread?, e: Throwable?) {
        try {
            if (e == null)
                return
            ErrorReporter.reportError(e, t)
        } catch(e: Throwable){
            e.printStackTrace()
            Bot.LOG.fatal("An error occurred when reporting an error.")
        }
    }
}