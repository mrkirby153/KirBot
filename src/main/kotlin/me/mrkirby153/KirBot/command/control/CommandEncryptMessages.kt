package me.mrkirby153.KirBot.command.control

import com.mrkirby153.bfs.sql.DB
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.promptForConfirmation

@Command(name = "encrypt-message", arguments = ["[message:string]"], admin = true)
class CommandEncryptMessages : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!cmdContext.has("message")) {
            val count = DB.getFirstColumn<Long>("SELECT COUNT(*) FROM server_messages")
            promptForConfirmation(context,
                    "Are you sure you want to encrypt `$count` messages?", {
                Bot.scheduler.submit({
                    context.channel.sendMessage(
                            "Encrypting messages, this may take some time").queue()
                    var m = 0
                    val results = DB.getResults("SELECT * FROM server_messages")
                    results.forEach { result ->
                        val msg = LogManager.encrypt(result.getString("message"))
                        if (msg != result.getString("message")) {
                            DB.executeUpdate("UPDATE server_messages SET message = ? WHERE id = ?",
                                    msg,
                                    result.getString("id"))
                            m++
                            if (m > 1 && (m % 1000) == 0) {
                                Bot.LOG.info("Encrypted $m/$count messages")
                            }
                        }
                    }
                    context.channel.sendMessage("Encrypted `$m` messages").queue()
                })
                true
            })
        } else {
            context.channel.sendMessage(
                    "`${LogManager.encrypt(cmdContext.get<String>("message")!!)}`").queue()
        }
    }
}

@Command(name = "decrypt-message", arguments = ["[message:string]"], admin = true)
class CommandDecryptMessage : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!cmdContext.has("message")) {
            val count = DB.getFirstColumn<Long>("SELECT COUNT(*) FROM server_messages")
            promptForConfirmation(context, "Are you sure you want to decrypt `$count` messages?", {
                Bot.scheduler.submit({
                    context.channel.sendMessage(
                            "Decrypting messages, this may take some time").queue()
                    var m = 0
                    val results = DB.getResults("SELECT * FROM server_messages")
                    results.forEach { result ->
                        val msg = LogManager.decrypt(result.getString("message"))
                        if (msg != result.getString("message")) {
                            DB.executeUpdate("UPDATE server_messages SET message = ? WHERE id = ?",
                                    msg,
                                    result.getString("id"))
                            m++
                            if (m > 1 && (m % 1000) == 0) {
                                Bot.LOG.info("Decrypted $m/$count messages")
                            }
                        }
                    }
                    context.channel.sendMessage("Decrypted `$m` messages").queue()
                })
                true
            })
        } else {
            context.channel.sendMessage(
                    "`${LogManager.decrypt(cmdContext.get<String>("message")!!)}`").queue()
        }
    }
}