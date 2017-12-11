package me.mrkirby153.KirBot.eval

import me.mrkirby153.KirBot.Bot
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.script.ScriptEngineManager


object Engine {
    private val service = Executors.newScheduledThreadPool(1,
            { it -> Thread(it).apply { name = "EvalThread" } })

    val defaultImports = arrayOf("java.lang", "java.io", "java.math",
            "java.util", "java.concurrent", "java.time")


    fun eval(fields: Map<String, Any>, imports: Array<String>, timeout: Long,
             script: String): Triple<Any?, String, String> {
        val toExecute = "with (new JavaImporter(" + imports.joinToString(
                ",") + ")) {" + script + "}"
        Bot.LOG.debug("Executing: $toExecute")


        val engine = ScriptEngineManager().getEngineByName("nashorn")

        fields.forEach {
            engine.put(it.key, it.value)
            Bot.LOG.debug("Variable ${it.key} = ${it.value}")
        }

        val outString = StringWriter()
        val outWriter = PrintWriter(outString)

        val errString = StringWriter()
        val errWriter = PrintWriter(errString)
        engine.context.errorWriter = errWriter
        engine.context.writer = outWriter

        val future = Engine.service.schedule({ engine.eval(toExecute) }, 0, TimeUnit.MILLISECONDS)

        var result: Any? = null

        try {
            result = future.get(timeout, TimeUnit.SECONDS)
        } catch (e: ExecutionException) {
            errWriter.println(e.cause.toString())
        } catch (e: TimeoutException) {
            future.cancel(true)
            errWriter.println(e.toString())
        } catch (e: InterruptedException) {
            future.cancel(true)
            errWriter.println(e.toString())
        }


        return Triple(result, outString.toString(), errString.toString())
    }
}