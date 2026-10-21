package com.orangecrave.admin.common.storage;

import com.orangecrave.admin.common.exception.BizException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * S3 存储分支。该实现默认不装配（storage.type=s3 才启用），故用 mock S3Client 锁住行为：
 * key 形态、上传确实发生、下发链接的两条回退、以及 key 越界必须拒绝。不连真实对象存储。
 */
class S3FileStorageTest {

    private S3Client s3;
    private S3FileStorage storage;

    @BeforeEach
    void setUp() {
        s3 = mock(S3Client.class);
        FileStorageProperties properties = new FileStorageProperties();
        FileStorageProperties.S3 config = properties.getS3();
        config.setEndpoint("http://localhost:9000");
        config.setBucket("orangecrave");
        storage = new S3FileStorage(s3, properties);
    }

    @Test
    void storeUploadsAndReturnsDatedKey() {
        String key = storage.store("art.png", new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)));

        assertThat(key).matches("\\d{8}/[0-9a-f]{32}\\.png");
        verify(s3).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void urlOfFallsBackToEndpointBucketKey() {
        assertThat(storage.urlOf("20261019/a.png"))
                .isEqualTo("http://localhost:9000/orangecrave/20261019/a.png");
    }

    @Test
    void urlOfPrefersPublicPrefixWhenConfigured() {
        FileStorageProperties properties = new FileStorageProperties();
        properties.getS3().setPublicUrlPrefix("https://cdn.example.com/");
        S3FileStorage custom = new S3FileStorage(s3, properties);

        assertThat(custom.urlOf("20261019/a.png")).isEqualTo("https://cdn.example.com/20261019/a.png");
    }

    @Test
    void invalidKeyIsRejectedBeforeTouchingStorage() {
        assertThatThrownBy(() -> storage.load("../etc/passwd"))
                .isInstanceOf(BizException.class);
        verifyNoInteractions(s3);
    }
}
