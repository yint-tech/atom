package cn.iinti.atom.utils

import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

object Md5Utils {
    // 这里名字不能是md5，要不然很多人会认为输出应该是字符串，导致使用错误
    @JvmStatic
    fun md5Bytes(key: String): ByteArray {
        try {
            val md5 = MessageDigest.getInstance("MD5")
            md5.update(key.toByteArray(StandardCharsets.UTF_8))
            return md5.digest()
        } catch (e: NoSuchAlgorithmException) {
            throw IllegalStateException("will not happen", e)
        }
    }

    @JvmStatic
    fun getHashWithInputStream(inputStream: InputStream): String {
        try {
            val buffer = ByteArray(1000)
            val md5 = MessageDigest.getInstance("MD5")

            var numRead: Int?
            while (true) {
                numRead = inputStream.read(buffer)
                if (numRead <= 0) {
                    break
                }
                md5.update(buffer, 0, numRead)
            }

            inputStream.close()
            return toHexString(md5.digest())
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }

    @JvmStatic
    fun seedLong(key: String): Long {
        val digest = md5Bytes(key)
        return (
                (digest[0].toInt() and 0xFF.toInt()).toLong() shl 56 or
                        ((digest[1].toInt() and 0xFF.toInt()).toLong() shl 48) or
                        ((digest[2].toInt() and 0xFF.toInt()).toLong() shl 40) or
                        ((digest[3].toInt() and 0xFF.toInt()).toLong() shl 32) or
                        ((digest[4].toInt() and 0xFF.toInt()).toLong() shl 24) or
                        ((digest[5].toInt() and 0xFF.toInt()).toLong() shl 16) or
                        ((digest[6].toInt() and 0xFF.toInt()).toLong() shl 8) or
                        ((digest[7].toInt() and 0xFF.toInt()).toLong())
                )
    }

    private val hexChar = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

    @JvmStatic
    fun md5Hex(input: String): String {
        return toHexString(md5Bytes(input))
    }

    @JvmStatic
    fun toHexString(b: ByteArray): String {
        return toHexString(b, 0, b.size)
    }

    @JvmStatic
    fun toHexString(b: ByteArray, start: Int, len: Int): String {
        val sb = StringBuilder(b.size * 2)
        for (i in 0 until len) {
            val b1 = b[start + i]
            sb.append(hexChar[(b1.toInt() and 240) shr 4])
            sb.append(hexChar[b1.toInt() and 15])
        }
        return sb.toString()
    }

    @JvmStatic
    fun hexToByteArray(input: String): ByteArray {
        var hexlen = input.length
        val result: ByteArray
        val inHex: String?
        if (hexlen % 2 == 1) {
            //奇数
            hexlen++
            result = ByteArray(hexlen / 2)
            inHex = "0" + input
        } else {
            //偶数
            result = ByteArray(hexlen / 2)
            inHex = input;
        }
        var j = 0
        for (i in 0 until hexlen step 2) {
            result[j] = hexToByte(inHex.substring(i, i + 2))
            j++
        }
        return result
    }

    @JvmStatic
    fun hexToByte(inHex: String): Byte {
        return Integer.parseInt(inHex, 16).toByte()
    }
}