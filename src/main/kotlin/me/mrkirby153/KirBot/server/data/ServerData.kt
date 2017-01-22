package me.mrkirby153.KirBot.server.data

import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.server.ServerRepository
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.child
import me.mrkirby153.KirBot.utils.createFileIfNotExist
import java.io.FileWriter

class ServerData(val server: Server) {

    val commands = mutableMapOf<String, CustomServerCommand>()

    var commandPrefix: String = "!"

    fun save() {
        val fileName = server.id + ".json"
        val file = ServerRepository.serverDirectory.child(fileName).createFileIfNotExist()
        val json = ServerRepository.gson.toJson(this)

        val writer = FileWriter(file)
        writer.write(json)
        writer.close()
    }

}

data class CustomServerCommand(val type: CommandType = CommandType.TEXT, val clearance: Clearance, val command: String)