package me.mrkirby153.KirBot.net

import me.mrkirby153.KirBot.Bot
import me.mrkirby153.KirBot.net.command.MinecraftBridge
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

object NetworkManager : Runnable {

    private val RANDOM = Random()

    var running: Boolean = false

    private var port: Int = 7563

    var activeConnections = mutableMapOf<String, NetworkConnection>()

    var messages = mutableMapOf<String, NetworkMessageHandler>()

    private val executor = Executors.newCachedThreadPool()

    private var managerThread: Thread? = null

    init {
        register("minecraftbridge", MinecraftBridge::class)
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
            executor.submit(networkConnection)
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

    fun stop() {
        executor.shutdown()
        try {
            Bot.LOG.info("Attempting graceful shutdown of thread pool")
            executor.shutdown()
            executor.awaitTermination(10, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Bot.LOG.info("Killing thread pool")
            executor.shutdownNow()
        }
        running = false
    }

    fun register(name: String, clazz: Class<out NetworkMessageHandler>) {
        val instance = clazz.newInstance()
        messages[name.toLowerCase()] = instance
    }

    fun register(name: String, clazz: KClass<out NetworkMessageHandler>) = register(name, clazz.java)

    fun getConnection(server: String): NetworkConnection? {
        for ((id, connection) in activeConnections) {
            if (connection.server?.id == server)
                return connection
        }
        return null
    }
}