
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import me.mrkirby153.KirBot.net.NetworkMessage
import me.mrkirby153.KirBot.net.command.BridgeMessage
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*

object NETWORK_TEST {

    lateinit var outputStream: OutputStream
    lateinit var inputStream: InputStream

    val gson: Gson = GsonBuilder().create()

    @JvmStatic fun main(args: Array<String>) {
        println("Attempting connection to network...")
        val socket = Socket("localhost", 7563)

        println("Connected, press enter to send command")
        readLine()

        outputStream = socket.outputStream
        inputStream = socket.inputStream

        write(NetworkMessage(Random().nextInt().toString(), "155424344447778816", "hG1JTOSbsR", "minecraftbridge", gson.toJson(BridgeMessage("271126974062264321", "mrkirby153", "Testing"))), true)

        println("Command sent, press enter to shut down")
        readLine()
        write(NetworkMessage("", "", "", "disconnect", ""), false)
        println("Closing socket and shutting down")
        socket.close()
    }

    fun write(message: String, response: Boolean) {
        val bytes = message.toByteArray()
        val size = ByteBuffer.allocate(4)
        size.order(ByteOrder.LITTLE_ENDIAN)
        size.putInt(bytes.size)

        outputStream.write(size.array())
        outputStream.write(bytes)
        outputStream.flush()

        if(response) {
            val networkMessage = read()
            if (networkMessage != null)
                println(networkMessage)
        }
    }

    fun write(any: Any, response: Boolean) {
        write(gson.toJson(any), response)
    }

    fun read(): NetworkMessage? {
        val rawSize = ByteArray(4)
        if (inputStream.read(rawSize) == -1) {
            println("Reached end of stream")
            return null
        }
        val msgLenBuff = ByteBuffer.wrap(rawSize)
        msgLenBuff.order(ByteOrder.LITTLE_ENDIAN)
        msgLenBuff.rewind()

        val size = msgLenBuff.int

        val rawMessage = ByteArray(size)
        inputStream.read(rawMessage)

        val json = String(rawMessage)

        val networkMessage = gson.fromJson(json, NetworkMessage::class.java)
        return networkMessage
    }
}
