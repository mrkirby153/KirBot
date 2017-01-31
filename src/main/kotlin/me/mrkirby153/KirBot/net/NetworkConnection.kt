package me.mrkirby153.KirBot.net

import com.google.gson.GsonBuilder
import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.server.Server
import me.mrkirby153.KirBot.server.ServerRepository
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NetworkConnection(val id: String, val socket: Socket) : Runnable {

    var running = true

    val gson = GsonBuilder().create()

    val inputStream: InputStream = socket.inputStream
    val outputStream: OutputStream = socket.outputStream

    var server: Server? = null


    override fun run() {
        while (running) {
            try {
                val rawSize = ByteArray(4)
                if (inputStream.read(rawSize) == -1) {
                    Bot.LOG.info("[Network $id] Reached end of stream for ${socket.inetAddress.hostAddress}:${socket.port}")
                    disconnect()
                    continue
                }
                val msgLenBuff = ByteBuffer.wrap(rawSize)
                msgLenBuff.order(ByteOrder.LITTLE_ENDIAN)
                msgLenBuff.rewind()

                val size = msgLenBuff.int

                val rawMessage = ByteArray(size)
                inputStream.read(rawMessage)

                val json = String(rawMessage)

                val networkMessage = gson.fromJson(json, NetworkMessage::class.java)

                if (networkMessage.messageType == "setServer") {
                    server = ServerRepository.getServer(Bot.jda.getGuildById(networkMessage.guild)) ?: continue
                    write(NetworkMessage(networkMessage.id, networkMessage.guild, null, "success", "Set Server"))
                }
                val handler = NetworkManager.messages[networkMessage.messageType.toLowerCase()] ?: continue
                if (handler.requireAuth) {
                    if (networkMessage.password == null)
                        continue
                    val password = networkMessage.password
                    val guild = Bot.jda.getGuildById(networkMessage.guild) ?: continue
                    val data = ServerRepository.getServer(guild)?.data() ?: continue
                    if (data.serverPassword == password) {
                        handleMessage(handler, networkMessage)
                    } else {
                        write(NetworkMessage(networkMessage.id, networkMessage.guild, null, "error", "Unauthorized"))
                    }
                } else {
                    handleMessage(handler, networkMessage)
                }

                if (networkMessage.messageType == "disconnect") {
                    Bot.LOG.info("[Network $id] ${socket.inetAddress.hostAddress}:${socket.port} closed connection")
                    disconnect()
                }

            } catch (e: Exception) {
                Bot.LOG.fatal("[Network $id] An error occurred on connection ${this.id} $e")
                disconnect()
            }
        }
    }

    private fun handleMessage(handler: NetworkMessageHandler, networkMessage: NetworkMessage) {
        try {
            handler.handle(networkMessage)
            write(NetworkMessage(networkMessage.id, networkMessage.guild, null, "success", "success"))
        } catch (e: Exception) {
            write(NetworkMessage(networkMessage.id, networkMessage.guild, null, "error", "An unknown error occurred when processing that message"))
        }
    }

    fun disconnect() {
        socket.shutdownInput()
        running = false
        NetworkManager.activeConnections.remove(id)
        socket.close()
    }

    fun terminate() {
        disconnect()
    }

    fun write(message: String) {
        println("Writing \"$message\"")
        val bytes = message.toByteArray()
        val size = ByteBuffer.allocate(4)
        size.order(ByteOrder.LITTLE_ENDIAN)
        size.putInt(bytes.size)

        if(!socket.isClosed) {
            outputStream.write(size.array())
            outputStream.write(bytes)
            outputStream.flush()
        }
    }

    fun write(any: Any) {
        write(gson.toJson(any))
    }
}

data class NetworkMessage(val id: String, val guild: String, val password: String?, val messageType: String, val data: String)