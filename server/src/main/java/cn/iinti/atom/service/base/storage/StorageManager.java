package cn.iinti.atom.service.base.storage;

import cn.iinti.atom.service.base.config.Settings;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * 统一管理文件资源，直接落本地磁盘（面向单体部署，不引入云存储抽象）
 */
@Slf4j
public class StorageManager {

    public static void store(String path, File file) {
        File targetFile = toLocalFile(path);
        try {
            if (!file.equals(targetFile)) {
                FileUtils.copyFile(file, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IllegalStateException("can not save file: " + path, e);
        }
    }

    public static File get(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        File file = toLocalFile(path);
        return file.exists() ? file : null;
    }

    public static void deleteFile(String path) {
        if (StringUtils.isBlank(path)) {
            return;
        }
        File targetFile = toLocalFile(path);
        if (targetFile.exists() && !targetFile.delete()) {
            log.error("can not remove file:{}", targetFile);
        }
        cleanEmptyDir(Settings.Storage.localStorage, targetFile.getParentFile());
    }

    /**
     * 文件删除后，父目录如果已经被清空，则递归清理掉，避免存储目录碎片化
     */
    private static void cleanEmptyDir(File root, File dir) {
        if (dir == null || dir.equals(root)) {
            return;
        }
        String[] list = dir.list();
        if (list == null || list.length > 0) {
            return;
        }
        FileUtils.deleteQuietly(dir);
        cleanEmptyDir(root, dir.getParentFile());
    }


    public static String retrieveContent(String path) {
        File file = get(path);
        if (file == null) {
            return "";
        }
        try {
            return FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("read file failed", e);
            return "";
        }
    }

    public static boolean storeWithViewPath(String path, String content) {
        File resultFile = null;
        try {
            resultFile = Files.createTempFile("result", ".atom").toFile();
            FileUtils.writeStringToFile(resultFile, content, StandardCharsets.UTF_8);
            store(path, resultFile);
            return true;
        } catch (Exception e) {
            log.error("save result failed", e);
            return false;
        } finally {
            FileUtils.deleteQuietly(resultFile);
        }
    }

    private static File toLocalFile(String path) {
        return new File(Settings.Storage.localStorage, path);
    }

}
