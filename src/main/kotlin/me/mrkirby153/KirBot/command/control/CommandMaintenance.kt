package me.mrkirby153.KirBot.command.control

import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.module.ModuleManager
import me.mrkirby153.KirBot.modules.Redis
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.deleteAfter
import me.mrkirby153.KirBot.utils.promptForConfirmation
import java.util.concurrent.TimeUnit

@Command(name = "clear-archives", control = true)
class CommandClearArchives : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        promptForConfirmation(context,
                ":warning: Are you sure you want to delete **all** archives? This cannot be undone.",
                {
                    doClean(context)
                    true
                }, {
            context.channel.sendMessage("Canceled!").queue {
                it.deleteAfter(10, TimeUnit.SECONDS)
                context.deleteAfter(10, TimeUnit.SECONDS)
            }
            true
        })
    }

    private fun doClean(context: Context) {
        ModuleManager[Redis::class.java].getConnection().use { con ->
            val keys = con.keys("archive:*")
            if(keys.isEmpty()){
                context.send().info(":warning: There are no archives to delete!").queue()
                return
            }
            val deleted = con.del(*(keys.toTypedArray()))
            context.send().success("Deleted `$deleted` archives", hand = true).queue()
        }
    }
}