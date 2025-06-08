package cn.iinti.katom.utils

import lombok.extern.slf4j.Slf4j
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.time.LocalDateTime
import java.time.ZoneOffset

@Slf4j
object CommonUtils {
    val zoneOffset = ZoneOffset.of("+8")

    
    fun throwableToString(throwable: Throwable): String {
        ByteArrayOutputStream().use { byteArrayOutputStream ->
            PrintStream(byteArrayOutputStream).use { printStream ->
                throwable.printStackTrace(printStream)
            }
            return byteArrayOutputStream.toString()
        }
    }

    
    fun dateTimeToTimestamp(ldt: LocalDateTime): Long {
        return ldt.toInstant(zoneOffset).toEpochMilli()
    }

    
    fun forceMkdir(dir: File): File {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw IllegalStateException("can not create dir: " + dir.getAbsolutePath())
                //log.warn("can not create dir:{}", dir.getAbsolutePath())
            }
        }
        return dir
    }
}