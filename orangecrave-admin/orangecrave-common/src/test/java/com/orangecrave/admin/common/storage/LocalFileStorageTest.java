package com.orangecrave.admin.common.storage;

import com.orangecrave.admin.common.exception.BizException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 下载接口在 Sa-Token 白名单里（图稿要被小程序直接引用），所以 key 的边界必须由存储层自己守住。 */
class LocalFileStorageTest {

    private LocalFileStorage storage(String dir) {
        FileStorageProperties properties = new FileStorageProperties();
        properties.setLocalDir(dir);
        return new LocalFileStorage(properties);
    }

    @Test
    void rejectsParentDirectoryEscape(@TempDir Path dir) {
        assertThatThrownBy(() -> storage(dir.toString()).load("../../secret.png"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void rejectsAbsoluteKey(@TempDir Path dir) {
        assertThatThrownBy(() -> storage(dir.toString()).load("/etc/passwd"))
                .isInstanceOf(BizException.class);
    }

    @Test
    void rejectsBlankKey(@TempDir Path dir) {
        assertThatThrownBy(() -> storage(dir.toString()).load("  "))
                .isInstanceOf(BizException.class);
    }

    @Test
    void storesAndReadsBackWithinRoot(@TempDir Path dir) throws Exception {
        LocalFileStorage storage = storage(dir.toString());
        String key = storage.store("art.png", new ByteArrayInputStream("图稿".getBytes(StandardCharsets.UTF_8)));
        assertThat(key).matches("\\d{8}/[0-9a-f]{32}\\.png");
        try (InputStream in = storage.load(key)) {
            assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("图稿");
        }
    }
}
