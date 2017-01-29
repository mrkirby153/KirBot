package me.mrkirby153.KirBot.server.data

import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.server.ServerRepository
import me.mrkirby153.KirBot.user.Clearance
import me.mrkirby153.KirBot.utils.child
import me.mrkirby153.KirBot.utils.createFileIfNotExist
import java.io.FileWriter
import java.security.SecureRandom

private val validStrings = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
class ServerData(@Transient var server: Server) {

    val commands = mutableMapOf<String, CustomServerCommand>()

    var commandPrefix: String = "!"

    var serverPassword = ""

    init {
        regeneratePassword()
    }

    fun save() {
        val fileName = server.id + ".json"
        val file = ServerRepository.serverDirectory.child(fileName).createFileIfNotExist()
        val json = ServerRepository.gson.toJson(this)

        val writer = FileWriter(file)
        writer.write(json)
        writer.flush()
        writer.close()
    }

    fun regeneratePassword() {
        val random = SecureRandom()
        var password = ""
        for(i in 1..10){
            password += validStrings[random.nextInt(validStrings.lastIndex)]
        }
        this.serverPassword = password
    }

}

data class CustomServerCommand(val type: CommandType = CommandType.TEXT, var clearance: Clearance, val command: String)