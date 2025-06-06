package cn.iinti.atom.utils

import cn.iinti.atom.entity.CommonRes
import com.alibaba.fastjson.JSON
import jakarta.servlet.ServletOutputStream
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.springframework.web.multipart.MultipartFile
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files

class ServletUtil {
    companion object {
        fun writeRes(responseHandler: HttpServletResponse, commonRes: CommonRes<*>) {
            responseHandler.contentType = "application/json;charset=utf8"
            try {
                val outputStream = responseHandler.outputStream
                outputStream.write(JSON.toJSONBytes(commonRes))
                outputStream.close()
            } catch (e: IOException) {
                // log.warn("writeRes error", e)
            }
        }

        interface FileUploadAction<R> {
            fun doAction(file: File): R
        }

        fun <R> uploadToTempNoCheck(multipartFile: MultipartFile, action: FileUploadAction<R>): R {
            return try {
                uploadToTemp(multipartFile, action)
            } catch (e: Exception) {
                throw IllegalStateException(e)
            }
        }

        @Throws(IOException::class)
        fun <R> uploadToTemp(multipartFile: MultipartFile, action: FileUploadAction<R>): R {
            val file = Files.createTempFile("upload", ".bin").toFile()
            try {
                multipartFile.transferTo(file)
                return action.doAction(file)
            } finally {
                FileUtils.deleteQuietly(file)
            }
        }

        fun responseFile(file: File?, contentType: String, httpServletResponse: HttpServletResponse) {
            responseFile(file, contentType, httpServletResponse, true)
        }

        fun responseFile(file: File?, contentType: String, httpServletResponse: HttpServletResponse, download: Boolean) {
            if (file == null || !file.canRead()) {
                writeRes(httpServletResponse, CommonRes.failed<Any>("system error,filed retrieve failed"))
                return
            }

            if (httpServletResponse.isCommitted) {
                // 文件同步可能需要时间，这个过程如果用户已经断开连接了
                return
            }

            httpServletResponse.characterEncoding = "UTF-8"
            if (download) {
                httpServletResponse.setHeader("Content-Disposition", "attachment;filename=${file.name}")
            }
            httpServletResponse.setHeader("Content-length", file.length().toString())
            httpServletResponse.contentType = contentType

            try {
                BufferedOutputStream(httpServletResponse.getOutputStream()).use { bufferedOutputStream ->
                    IOUtils.copy(Files.newInputStream(file.toPath()), bufferedOutputStream)
                }
            } catch (e: IOException) {
                // 此时已经在写数据了,不能再返回其他数据
                // log.error("write download file error", e)
            }
        }
    }
}