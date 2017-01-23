package me.mrkirby153.KirBot.net

import me.mrkirby153.KirBot.Bot
import java.net.ServerSocket
import java.util.*

object NetworkManager : Runnable {

    private val RANDOM = Random()

    var running: Boolean = false

    private var port: Int = 7563

    var activeConnections = mutableMapOf<String, NetworkConnection>()

    var messages = mutableMapOf<String, NetworkMessageHandler>()

    private var managerThread: Thread? = null

    init {

    }

    override fun run() {
        Bot.LOG.info("Starting network manager...")
        val serverSocket = ServerSocket(port)
        while (running) {
            val clientSocket = serverSocket.accept()
            val id = RANDOM.nextInt(99999).toString()
            Bot.LOG.info("Connect from ${clientSocket.inetAddress.hostAddress}:${clientSocket.port} (ID: $id)")
            val networkConnection = NetworkConnection(id, clientSocket)
            activeConnections[id] = networkConnection
        }
    }

    @JvmOverloads
    fun start(port: Int = 7563) {
        if (running)
            error("The network manager is already started!")
        this.port = port
        running = true
        managerThread = Thread(this)
        managerThread?.isDaemon = true
        managerThread?.name = "NetworkManager"
        managerThread?.start()
    }

    fun stop(){
        activeConnections.forEach {
            it.value.disconnect()
        }
        running = false
    }

    fun register(name: String, clazz: Class<out NetworkMessageHandler>) {
        val instance = clazz.newInstance()
        messages[name] = instance
    }
}