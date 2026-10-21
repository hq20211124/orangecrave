package com.orangecrave.admin.controller;

import com.orangecrave.admin.common.exception.BizException;
import com.orangecrave.admin.common.result.Result;
import com.orangecrave.admin.common.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传/下载（见 02 篇 §2.11 / §2.13）。走 FileStorage 适配器，v1 本地实现。  */
@RestController
@RequiredArgsConstructor
public class FileController {

    private final FileStorage fileStorage;

    @PostMapping({"/admin-api/file/upload", "/client-api/file/upload"})
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            String key = fileStorage.store(file.getOriginalFilename(), in);
            Map<String, String> data = new HashMap<>();
            data.put("key", key);
            data.put("url", fileStorage.urlOf(key));
            return Result.ok(data);
        } catch (IOException e) {
            throw new BizException("上传失败: " + e.getMessage());
        }
    }

    @GetMapping({"/admin-api/file/download", "/client-api/file/download"})
    public ResponseEntity<InputStreamResource> download(@RequestParam("key") String key) {
        InputStream in = fileStorage.load(key);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + key + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(in));
    }
}
