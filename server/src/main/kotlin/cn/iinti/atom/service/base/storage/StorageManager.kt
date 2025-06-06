package cn.iinti.atom.service.base.storage

import cn.iinti.atom.service.base.config.Settings
import org.slf4j.LoggerFactory
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * 统一管理文件资源
 */
object StorageManager {
    private val log = LoggerFactory.getLogger(StorageManager::class.java)
    private val localStorage = LocalStorage()

    fun store(path: String, file: File) {
        try {
            localStorage.store(path, file)
        } catch (e: IOException) {
            throw IllegalStateException("can not save file: $path", e)
        }
    }

    fun get(path: String): File? {
        val file = getImpl(path)
        if (file == null) {
            return null
        }
        if (!file.exists()) {
            return null
        }
        return file
    }

    private fun getImpl(path: String): File? {
        if (StringUtils.isBlank(path)) {
            return null
        }
        return try {
            val file = localStorage.getFile(path, null)
            if (file != null && file.exists()) {
                file
            } else {
                null
            }
        } catch (e: IOException) {
            throw IllegalStateException("can not get file: $path")
        }
    }

    fun deleteFile(path: String) {
        if (StringUtils.isBlank(path)) {
            return
        }
        localStorage.delete(path)
    }

    fun retrieveContent(filePathView: String): String {
        val file = get(filePathView)
        if (file == null || !file.exists()) {
            return ""
        }
        return try {
            FileUtils.readFileToString(file, StandardCharsets.UTF_8)
        } catch (e: IOException) {
            log.error("read file failed", e)
            ""
        }
    }

    fun storeWithViewPath(filePathView: String, content: String): Boolean {
        var resultFile: File? = null
        try {
            resultFile = Files.createTempFile("result", ".atom").toFile()
            FileUtils.writeStringToFile(resultFile, content, StandardCharsets.UTF_8)
            store(filePathView, resultFile)
            return true
        } catch (e: Exception) {
            log.error("save result failed", e)
            return false
        } finally {
            FileUtils.deleteQuietly(resultFile)
        }
    }
}