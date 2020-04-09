package me.mrkirby153.KirBot.command.control

import com.mrkirby153.bfs.sql.DB
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.command.annotations.AdminCommand
import me.mrkirby153.KirBot.command.annotations.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.utils.Context
import me.mrkirby153.KirBot.utils.promptForConfirmation


class CommandEncryptMessages {

    @AdminCommand
    @Command(name = "encrypt-message", arguments = ["[message:string]"])
    fun execute(context: Context, cmdContext: CommandContext) {
        if (!cmdContext.has("message")) {
            val count = DB.getFirstColumn<Long>("SELECT COUNT(*) FROM server_messages")
            promptForConfirmation(context,
                    "Are you sure you want to encrypt `$count` messages?") {
                Bot.scheduler.submit {
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
                }
                true
            }
        } else {
            context.channel.sendMessage(
                    "`${LogManager.encrypt(cmdContext.get<String>("message")!!)}`").queue()
        }
    }
}


class CommandDecryptMessage{

    @AdminCommand
    @Command(name = "decrypt-message", arguments = ["[message:string]"])
    fun execute(context: Context, cmdContext: CommandContext) {
        if (!cmdContext.has("message")) {
            val count = DB.getFirstColumn<Long>("SELECT COUNT(*) FROM server_messages WHERE message LIKE 'e:%'")
            val attachCount = DB.getFirstColumn<Long>("SELECT COUNT(*) FROM attachments WHERE attachments LIKE 'e:%'")
            promptForConfirmation(context, "Are you sure you want to decrypt `$count` messages?", {
                Bot.scheduler.submit {
                    context.channel.sendMessage(
                            "Decrypting messages, this may take some time").queue()
                    var m = 0
                    var a = 0
                    val results = DB.getResults("SELECT * FROM server_messages WHERE message LIKE 'e:%'")
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
                    val attachments = DB.getResults("SELECT * FROM attachments WHERE attachments LIKE 'e:%'")
                    attachments.forEach { attachment ->
                        val attach = LogManager.decrypt(attachment.getString("attachments"))
                        DB.executeUpdate("UPDATE attachments SET attachments = ? WHERE id = ?", attach, attachment.getString("id"))
                        a++
                        if(a > 1 && (a % 100) == 0) {
                            Bot.LOG.info("Decrypted $a/$attachCount attachments")
                        }
                    }
                    context.channel.sendMessage("Decrypted `$a` attachments").queue()

                }
                true
            })
        } else {
            context.channel.sendMessage(
                    "`${LogManager.decrypt(cmdContext.get<String>("message")!!)}`").queue()
        }
    }
}