package me.mrkirby153.KirBot.server.data

import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.server.ServerRepository
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.child
import me.mrkirby153.KirBot.utils.createFileIfNotExist
import java.io.FileWriter

private val validStrings = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
class ServerData(@Transient var server: Server) {

    val commands = mutableMapOf<String, CustomServerCommand>()

    var commandPrefix: String = "!"

    fun save() {
        val fileName = server.id + ".json"
        val file = ServerRepository.serverDirectory.child(fileName).createFileIfNotExist()
        val json = ServerRepository.gson.toJson(this)

        val writer = FileWriter(file)
        writer.write(json)
        writer.flush()
        writer.close()
    }

}

data class CustomServerCommand(val type: CommandType = CommandType.TEXT, var clearance: Clearance, val command: String)