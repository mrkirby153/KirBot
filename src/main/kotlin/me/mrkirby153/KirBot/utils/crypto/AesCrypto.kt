package me.mrkirby153.KirBot.utils.crypto

import me.mrkirby153.KirBot.Bot
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.binary.Hex
import org.json.JSONObject
import org.json.JSONTokener
import java.nio.charset.Charset
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec



object AesCrypto {

    private val key by lazy {
        Base64.decodeBase64(Bot.properties.getProperty("encryption-key") ?: throw Exception("no key provided"))
    }

    fun encrypt(plaintext: String, serialize: Boolean = true) : String {
        val key = SecretKeySpec(this.key, "AES")

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")

        cipher.init(Cipher.ENCRYPT_MODE, key)

        val serializedPlaintext = "s:" + plaintext.toByteArray().size + ":\"" + plaintext + "\";"

        val toUse = if(serialize) serializedPlaintext else plaintext
        val iv = cipher.iv
        val encVal = cipher.doFinal(toUse.toByteArray())

        val encrypted = Base64.encodeBase64String(encVal)

        val macKey = SecretKeySpec(this.key, "HmacSHA256")
        val hmacSha256 = Mac.getInstance("HmacSha256")

        hmacSha256.init(macKey)
        hmacSha256.update(Base64.encodeBase64(iv))

        val calcMac = hmacSha256.doFinal(encrypted.toByteArray())

        val mac = String(Hex.encodeHex(calcMac))

        val json = JSONObject()

        json.put("iv", Base64.encodeBase64String(iv))
        json.put("value", encrypted)
        json.put("mac", mac)

        return Base64.encodeBase64String(json.toString().toByteArray())
    }

    fun decrypt(ivValue: String, encryptedData: String, macValue: String): String {
        val key = SecretKeySpec(this.key, "AES")
        val iv = Base64.decodeBase64(ivValue.toByteArray())
        val decodedValue = Base64.decodeBase64(encryptedData.toByteArray())

        val macKey = SecretKeySpec(this.key, "HmacSHA256")
        val hmacSha256 = Mac.getInstance("HmacSHA256")
        hmacSha256.init(macKey)
        hmacSha256.update(ivValue.toByteArray())
        val calcMac = hmacSha256.doFinal(encryptedData.toByteArray())
        val mac = Hex.decodeHex(macValue.toCharArray())
        if (!Arrays.equals(calcMac, mac))
            throw IllegalArgumentException("MAC Mismatch")

        val c = Cipher.getInstance("AES/CBC/PKCS5Padding") // or PKCS5Padding
        c.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        val decValue = c.doFinal(decodedValue)

        var firstQuoteIndex = 0
        while (decValue[firstQuoteIndex] != '"'.toByte()) firstQuoteIndex++
        return String(Arrays.copyOfRange(decValue, firstQuoteIndex + 1, decValue.size - 2))
    }

    fun decrypt(encData: String): String {
        val rawJson = Base64.decodeBase64(encData).toString(Charset.defaultCharset())
        println(rawJson)
        val json = JSONObject(JSONTokener(rawJson))

        return decrypt(json.getString("iv"), json.getString("value"), json.getString("mac"))
    }
}