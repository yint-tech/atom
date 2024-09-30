package cn.iinti.atom.utils;

import cn.iinti.atom.entity.CommonRes;
import com.alibaba.fastjson.JSON;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
public class ServletUtil {
    public static void writeRes(HttpServletResponse responseHandler, CommonRes<?> commonRes) {
        responseHandler.setContentType("application/json;charset=utf8");
        try {
            ServletOutputStream outputStream = responseHandler.getOutputStream();
            outputStream.write(JSON.toJSONBytes(commonRes));
            outputStream.close();
        } catch (IOException e) {
            log.warn("writeRes error", e);
        }
    }


    public interface FileUploadAction<R> {
        R doAction(File file);
    }

    public static <R> R uploadToTempNoCheck(MultipartFile multipartFile, FileUploadAction<R> action) {
        try {
            return uploadToTemp(multipartFile, action);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static <R> R uploadToTemp(MultipartFile multipartFile, FileUploadAction<R> action) throws IOException {
        File file = Files.createTempFile("upload", ".bin").toFile();
        try {
            multipartFile.transferTo(file);
            return action.doAction(file);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    public static void responseFile(File file, String contentType, HttpServletResponse httpServletResponse) {
        responseFile(file, contentType, httpServletResponse, true);
    }

    public static void responseFile(File file, String contentType, HttpServletResponse httpServletResponse, boolean download) {
        if (file == null || !file.canRead()) {
            writeRes(httpServletResponse, CommonRes.failed("system error,filed retrieve failed"));
            return;
        }

        if (httpServletResponse.isCommitted()) {
            // 文件同步可能需要时间，这个过程如果用户已经断开连接了
            return;
        }

        httpServletResponse.setCharacterEncoding("UTF-8");
        if (download) {
            httpServletResponse.setHeader("Content-Disposition", "attachment;filename=" + file.getName());
        }
        httpServletResponse.setHeader("Content-length", String.valueOf(file.length()));
        httpServletResponse.setContentType(contentType);

        try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(httpServletResponse.getOutputStream())) {
            IOUtils.copy(Files.newInputStream(file.toPath()), bufferedOutputStream);
        } catch (IOException e) {
            // 此时已经在写数据了,不能再返回其他数据
            log.error("write download file error", e);
        }
    }
}
