package cn.iinti.katom.utils

import org.apache.commons.io.IOUtils
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

class ResourceUtil {
    companion object {
        
        fun readLines(resourceName: String): List<String> {
            return try {
                IOUtils.readLines(openResource(resourceName), StandardCharsets.UTF_8)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        
        fun readText(resourceName: String): String {
            return try {
                IOUtils.toString(openResource(resourceName), StandardCharsets.UTF_8)
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        
        fun readBytes(resourceName: String): ByteArray {
            return try {
                IOUtils.toByteArray(openResource(resourceName))
            } catch (e: IOException) {
                throw RuntimeException(e)
            }
        }

        
        fun openResource(name: String): InputStream {
            val resource = ResourceUtil::class.java.classLoader.getResourceAsStream(name)
                ?: throw IllegalStateException("can not find resource: $name")
            return resource
        }
    }
}