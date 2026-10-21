package com.orangecrave.admin.common.storage;

import com.orangecrave.admin.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 对象存储实现（S3 兼容协议）。一套代码同时吃 MinIO / 阿里云 OSS / Ceph——
 * 换存储只改配置，不动业务。仅当 {@code orangecrave.storage.type=s3} 时装配。
 *
 * <p>上传体量受 multipart 上限约束（当前 20MB），故直接读全量字节再 PUT；
 * 若将来放开大文件，改用分片上传（MultipartUpload）而非加大这个缓冲。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "orangecrave.storage", name = "type", havingValue = "s3")
public class S3FileStorage implements FileStorage {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final S3Client s3;
    private final FileStorageProperties.S3 config;

    public S3FileStorage(S3Client s3, FileStorageProperties properties) {
        this.s3 = s3;
        this.config = properties.getS3();
    }

    @Override
    public String store(String originalFilename, InputStream in) {
        String key = buildKey(originalFilename);
        try {
            s3.putObject(PutObjectRequest.builder()
                            .bucket(config.getBucket())
                            .key(key)
                            .build(),
                    RequestBody.fromBytes(in.readAllBytes()));
        } catch (IOException e) {
            throw new BizException("文件读取失败: " + e.getMessage());
        } catch (Exception e) {
            throw new BizException("文件上传失败: " + e.getMessage());
        }
        return key;
    }

    @Override
    public InputStream load(String key) {
        assertKey(key);
        try {
            return s3.getObject(GetObjectRequest.builder()
                    .bucket(config.getBucket())
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            throw new BizException(404, "文件不存在");
        } catch (Exception e) {
            throw new BizException("文件读取失败: " + e.getMessage());
        }
    }

    @Override
    public String urlOf(String key) {
        String prefix = config.getPublicUrlPrefix();
        if (prefix != null && !prefix.isBlank()) {
            return trimSlash(prefix) + "/" + key;
        }
        // 未配对外域名时回退「端点 + 桶 + key」，适合 MinIO 本地直连
        String endpoint = config.getEndpoint();
        if (endpoint != null && !endpoint.isBlank()) {
            return trimSlash(endpoint) + "/" + config.getBucket() + "/" + key;
        }
        return key;
    }

    private String buildKey(String originalFilename) {
        String ext = "";
        if (originalFilename != null) {
            int dot = originalFilename.lastIndexOf('.');
            if (dot >= 0) {
                ext = originalFilename.substring(dot);
            }
        }
        return LocalDate.now().format(DAY) + "/" + UUID.randomUUID().toString().replace("-", "") + ext;
    }

    /** key 由本工程生成，但下载接口在白名单里，仍拒绝越界形态以防被手工构造。 */
    private void assertKey(String key) {
        if (key == null || key.isBlank() || key.startsWith("/") || key.contains("\\") || key.contains("..")) {
            throw new BizException(400, "文件 key 不合法");
        }
    }

    private String trimSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
