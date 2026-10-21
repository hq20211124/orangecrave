package com.orangecrave.admin.common.storage;

import com.orangecrave.admin.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地文件存储实现。key 形如 {@code 20261015/uuid.png}。
 * 仅当 {@code orangecrave.storage.type=local}（默认）时装配；切 s3 时由 {@link S3FileStorage} 接管。  */
@Component
@ConditionalOnProperty(prefix = "orangecrave.storage", name = "type", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor
public class LocalFileStorage implements FileStorage {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final FileStorageProperties properties;

    @Override
    public String store(String originalFilename, InputStream in) {
        String ext = "";
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0) {
                ext = originalFilename.substring(dot);
            }
        }
        String key = LocalDate.now().format(DAY) + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BizException("文件保存失败: " + e.getMessage());
        }
        return key;
    }

    @Override
    public InputStream load(String key) {
        Path target = resolve(key);
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new BizException(404, "文件不存在");
        }
    }

    /**
     * 下载接口在 Sa-Token 白名单里（图稿需要被小程序直接引用），所以 key 必须自己守住边界：
     * 归一化后仍要落在 localDir 之内，否则 ../ 可以读任意文件。
     */
    private Path resolve(String key) {
        if (key == null || key.isBlank() || key.startsWith("/") || key.contains("\\") || key.charAt(0) == '.') {
            throw new BizException(400, "文件 key 不合法");
        }
        Path base = Paths.get(properties.getLocalDir()).toAbsolutePath().normalize();
        Path target = base.resolve(key).normalize();
        if (!target.startsWith(base)) {
            throw new BizException(400, "文件 key 不合法");
        }
        return target;
    }

    @Override
    public String urlOf(String key) {
        return properties.getUrlPrefix() + key;
    }
}
