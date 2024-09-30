package cn.iinti.atom.utils;

import cn.iinti.atom.entity.CommonRes;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Slf4j
public class CommonUtils {

    public static final ZoneOffset zoneOffset = ZoneOffset.of("+8");

    public static String throwableToString(Throwable throwable) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (PrintStream printStream = new PrintStream(byteArrayOutputStream)) {
            throwable.printStackTrace(printStream);
        }
        return byteArrayOutputStream.toString();
    }


    public long dateTimeToTimestamp(LocalDateTime ldt) {
        return ldt.toInstant(zoneOffset).toEpochMilli();
    }


    public static File forceMkdir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IllegalStateException("can not create dir: " + dir.getAbsolutePath());
                //log.warn("can not create dir:{}", dir.getAbsolutePath());
            }
        }
        return dir;
    }
}
