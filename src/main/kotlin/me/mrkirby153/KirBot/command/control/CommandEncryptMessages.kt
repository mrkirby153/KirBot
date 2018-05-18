package me.mrkirby153.KirBot.command.control

import co.aikar.idb.DB
import me.mrkirby153.KirBot.command.BaseCommand
import me.mrkirby153.KirBot.command.Command
import me.mrkirby153.KirBot.command.args.CommandContext
import me.mrkirby153.KirBot.logger.LogManager
import me.mrkirby153.KirBot.utils.Context

@Command(name = "encrypt-message", arguments = ["[message:string]"], control = true)
class CommandEncryptMessages : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!cmdContext.has("message")) {
            context.channel.sendMessage("Encrypting messages, this may take some time").queue()
            var m = 0
            val results = DB.getResults("SELECT * FROM server_messages")
            results.forEach { result ->
                val msg = LogManager.encrypt(result.getString("message"))
                if (msg != result.getString("message")) {
                    DB.executeUpdate("UPDATE server_messages SET message = ? WHERE id = ?", msg,
                            result.getString("id"))
                    m++
                }
            }
            context.channel.sendMessage("Encrypted `$m` messages").queue()
        } else {
            context.channel.sendMessage(
                    "`${LogManager.encrypt(cmdContext.get<String>("message")!!)}`").queue()
        }
    }
}

@Command(name = "decrypt-message", arguments = ["[message:string]"], control = true)
class CommandDecryptMessage : BaseCommand() {

    override fun execute(context: Context, cmdContext: CommandContext) {
        if (!cmdContext.has("message")) {
            context.channel.sendMessage("Decrypting messages, this may take some time").queue()
            var m = 0
            val results = DB.getResults("SELECT * FROM server_messages")
            results.forEach { result ->
                val msg = LogManager.decrypt(result.getString("message"))
                if (msg != result.getString("message")) {
                    DB.executeUpdate("UPDATE server_messages SET message = ? WHERE id = ?", msg,
                            result.getString("id"))
                    m++
                }
            }
            context.channel.sendMessage("Decrypted `$m` messages").queue()
        } else {
            context.channel.sendMessage(
                    "`${LogManager.decrypt(cmdContext.get<String>("message")!!)}`").queue()
        }
    }
}