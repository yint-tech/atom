package cn.iinti.katom.utils

import cn.iinti.katom.base.config.Settings
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.net.NetworkInterface
import java.net.SocketException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*


object ServerIdentifier {
    private const val SERVER_ID_FILE_NAME = "atom_server_id.txt"
    private const val UN_RESOLVE = "un_resolve_"
    private val log = LoggerFactory.getLogger(ServerIdentifier::class.java)

    @Volatile
    private var clientIdInMemory: String? = null

    fun setupId(id: String?) {
        if (id.isNullOrBlank()) {
            return
        }
        clientIdInMemory = id
        val file = resolveIdCacheFile()
        try {
            FileUtils.writeStringToFile(file, clientIdInMemory, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            log.error("error to write file", e)
            throw RuntimeException(e)
        }
    }

    
    fun id(): String {
        clientIdInMemory?.let { return it }
        // from cache file
        val file = resolveIdCacheFile()
        log.info("serverIdFile: " + file.absolutePath)
        if (file.exists()) {
            try {
                val s = IOUtils.toString(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)
                if (!s.isNullOrBlank() && !s.startsWith(UN_RESOLVE)) {
                    clientIdInMemory = s
                    return s
                }
            } catch (e: IOException) {
                log.error("can not read id file: " + file.absolutePath, e)
            }
        }

        clientIdInMemory = generateClientId() + "_" + Random().nextInt(10000)
        try {
            FileUtils.writeStringToFile(file, clientIdInMemory, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            log.error("error to write file", e)
            throw RuntimeException(e)
        }
        return clientIdInMemory!!
    }

    private fun generateClientId(): String {
        val mac = generateClientIdForNormalJVM()
        return if (StringUtils.isNotEmpty(mac)) {
            mac!!
        } else {
            "$UN_RESOLVE${UUID.randomUUID()}"
        }
    }

    private fun generateClientIdForNormalJVM(): String? {
        return try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                if (networkInterface.isVirtual || networkInterface.isLoopback) {
                    continue
                }

                val hardwareAddress = networkInterface.hardwareAddress ?: continue
                return "${parseByte(hardwareAddress[0])}:${parseByte(hardwareAddress[1])}:${parseByte(hardwareAddress[2])}:${
                    parseByte(
                        hardwareAddress[3]
                    )
                }:${parseByte(hardwareAddress[4])}:${parseByte(hardwareAddress[5])}"
            }
            null
        } catch (e: SocketException) {
            null
        }
    }

    private fun parseByte(b: Byte): String {
        val intValue = if (b >= 0) b.toInt() else 256 + b.toInt()
        return Integer.toHexString(intValue)
    }

    private fun resolveIdCacheFile(): File {
        return resolveJvmEnvCacheIdFile()
    }

    @Volatile
    private var jvmEnvCacheIdFile: File? = null

    private fun resolveJvmEnvCacheIdFile(): File {
        if (jvmEnvCacheIdFile != null) {
            return jvmEnvCacheIdFile!!
        }
        jvmEnvCacheIdFile = File(Settings.Storage.root, SERVER_ID_FILE_NAME)
        return jvmEnvCacheIdFile!!
    }
}