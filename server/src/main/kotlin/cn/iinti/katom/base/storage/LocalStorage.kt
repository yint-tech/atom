package cn.iinti.katom.base.storage

import cn.iinti.katom.base.config.Settings
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.StandardCopyOption
import org.apache.commons.io.FileUtils

class LocalStorage : IStorage {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun store(path: String, file: File) {
        val targetFile = File(Settings.Storage.localStorage, path)
        if (file == targetFile) {
            return
        }
        FileUtils.copyFile(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
    }

    override fun delete(path: String) {
        val targetFile = File(Settings.Storage.localStorage, path)
        if (targetFile.exists()) {
            if (!targetFile.delete()) {
                log.error("can not remove file:{}", targetFile)
            }
        }
        cleanEmptyDir(Settings.Storage.localStorage, targetFile.parentFile)
    }

    private fun cleanEmptyDir(root: File, dir: File) {
        if (dir == root) {
            return
        }
        val list = dir.list()
        if (list == null || list.isNotEmpty()) {
            return
        }
        FileUtils.deleteQuietly(dir)
        cleanEmptyDir(root, dir.getParentFile())
    }

    override fun exist(path: String): Boolean {
        return File(Settings.Storage.localStorage, path).exists()
    }

    override fun getFile(path: String, destinationFile: File?): File {
        val file = File(Settings.Storage.localStorage, path)
        val destFile = destinationFile ?: file
        if (!file.exists()) {
            return file
        }
        if (file == destFile) {
            return file
        }
        FileUtils.copyFile(file, destFile)
        return destFile
    }

    override fun genUrl(path: String): String {
        TODO("not support now")
    }

    override fun available(): Boolean {
        return true
    }

    override fun id(): String {
        return "local"
    }
}